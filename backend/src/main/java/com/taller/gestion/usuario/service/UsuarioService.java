package com.taller.gestion.usuario.service;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.common.exception.OperacionNoPermitidaException;
import com.taller.gestion.common.exception.RecursoDuplicadoException;
import com.taller.gestion.common.exception.RecursoNoEncontradoException;
import com.taller.gestion.usuario.dto.UsuarioCreateDTO;
import com.taller.gestion.usuario.dto.UsuarioPasswordDTO;
import com.taller.gestion.usuario.dto.UsuarioResponseDTO;
import com.taller.gestion.usuario.dto.UsuarioUpdateDTO;
import com.taller.gestion.usuario.entity.Rol;
import com.taller.gestion.usuario.entity.Usuario;
import com.taller.gestion.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final int PASSWORD_MIN = 8;
    private static final int PASSWORD_MAX_BYTES = 72;
    private static final AuthenticationTrustResolver TRUST_RESOLVER =
            new AuthenticationTrustResolverImpl();

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<UsuarioResponseDTO> buscar(String busqueda, boolean incluirInactivos, Pageable pageable) {
        String texto = busqueda == null ? "" : busqueda.trim();
        return usuarioRepository.buscar(texto, incluirInactivos, pageable)
                .map(this::mapearAResponse);
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO findById(Long id_usuario) {
        return mapearAResponse(obtenerPorId(id_usuario));
    }

    /**
     * Punto de acceso para otros módulos (USU-01): permite validar y asociar un usuario
     * sin que accedan directamente a {@link UsuarioRepository}.
     */
    @Transactional(readOnly = true)
    public Usuario obtenerPorId(Long id_usuario) {
        return usuarioRepository.findById(id_usuario)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado con id " + id_usuario));
    }

    /**
     * Punto de acceso para otros módulos: devuelve la entidad del usuario logueado en la
     * sesión actual. Se usa el email como nombre de usuario porque así lo carga
     * {@link UsuarioUserDetailsService}.
     */
    @Transactional(readOnly = true)
    public Usuario obtenerAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || TRUST_RESOLVER.isAnonymous(authentication)) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Se requiere iniciar sesión para realizar esta operación");
        }

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario autenticado no encontrado: " + authentication.getName()));
        if (!usuario.isActivo()) {
            throw new OperacionNoPermitidaException("El usuario autenticado está desactivado");
        }
        return usuario;
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPorEmail(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email)
                .map(this::mapearAResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }

    @Transactional
    public UsuarioResponseDTO save(UsuarioCreateDTO dto) {
        validarPassword(dto.password());
        validarEmailUnico(dto.email(), null);

        Usuario usuario = new Usuario();
        usuario.setNombre(dto.nombre());
        usuario.setEmail(dto.email());
        usuario.setPassword(passwordEncoder.encode(dto.password()));
        usuario.setRol(dto.rol());
        usuario.setActivo(true);

        return mapearAResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponseDTO update(Long id_usuario, UsuarioUpdateDTO dto) {
        Usuario usuario = obtenerPorId(id_usuario);

        if (dto.email() != null && !dto.email().equalsIgnoreCase(usuario.getEmail())) {
            validarEmailUnico(dto.email(), id_usuario);
            usuario.setEmail(dto.email());
        }
        if (dto.nombre() != null) {
            usuario.setNombre(dto.nombre());
        }

        Rol rolFinal = dto.rol() != null ? dto.rol() : usuario.getRol();
        validarNoDejarSinAdministradores(usuario, rolFinal, usuario.isActivo());
        usuario.setRol(rolFinal);

        return mapearAResponse(usuario);
    }

    @Transactional
    public UsuarioResponseDTO cambiarEstado(Long id_usuario, boolean activo) {
        Usuario usuario = obtenerPorId(id_usuario);

        if (!activo && esUsuarioAutenticado(usuario.getEmail())) {
            throw new OperacionNoPermitidaException(
                    "No se puede desactivar el usuario con el que se está autenticado");
        }
        validarNoDejarSinAdministradores(usuario, usuario.getRol(), activo);
        usuario.setActivo(activo);

        return mapearAResponse(usuario);
    }

    @Transactional
    public void cambiarPassword(Long id_usuario, UsuarioPasswordDTO dto) {
        validarPassword(dto.password());
        Usuario usuario = obtenerPorId(id_usuario);
        usuario.setPassword(passwordEncoder.encode(dto.password()));
    }

    private void validarPassword(String password) {
        if (password == null || password.length() < PASSWORD_MIN) {
            throw new DatosInvalidosException(
                    "La contraseña debe tener al menos " + PASSWORD_MIN + " caracteres");
        }
        // BCrypt solo considera los primeros 72 bytes de la contraseña.
        if (password.getBytes(StandardCharsets.UTF_8).length > PASSWORD_MAX_BYTES) {
            throw new DatosInvalidosException(
                    "La contraseña no puede superar los " + PASSWORD_MAX_BYTES + " bytes");
        }
    }

    private void validarEmailUnico(String email, Long idExcluir) {
        boolean duplicado = idExcluir == null
                ? usuarioRepository.existsByEmailIgnoreCase(email)
                : usuarioRepository.countByEmailAndIdNot(email, idExcluir) > 0;
        if (duplicado) {
            throw new RecursoDuplicadoException("Ya existe un usuario con ese email");
        }
    }

    private void validarNoDejarSinAdministradores(Usuario usuario, Rol rolNuevo, boolean activoNuevo) {
        boolean eraAdminActivo = usuario.getRol() == Rol.ADMINISTRADOR && usuario.isActivo();
        boolean sigueAdminActivo = rolNuevo == Rol.ADMINISTRADOR && activoNuevo;
        if (eraAdminActivo && !sigueAdminActivo
                && usuarioRepository.countByRolAndActivoTrue(Rol.ADMINISTRADOR) <= 1) {
            throw new OperacionNoPermitidaException(
                    "Debe quedar al menos un administrador activo");
        }
    }

    private boolean esUsuarioAutenticado(String email) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getName() != null
                && authentication.getName().equalsIgnoreCase(email);
    }

    private UsuarioResponseDTO mapearAResponse(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getId_usuario(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRol(),
                usuario.isActivo());
    }
}

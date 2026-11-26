package com.taller.gestion.auditoria.service;

import com.taller.gestion.auditoria.dto.AuditoriaCreateDTO;
import com.taller.gestion.auditoria.dto.AuditoriaResponseDTO;
import com.taller.gestion.auditoria.entity.AccionAuditoria;
import com.taller.gestion.auditoria.entity.Auditoria;
import com.taller.gestion.auditoria.entity.OrigenAuditoria;
import com.taller.gestion.auditoria.repository.AuditoriaRepository;
import com.taller.gestion.common.exception.RecursoNoEncontradoException;
import com.taller.gestion.usuario.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private static final String SISTEMA = "SISTEMA";
    private static final AuthenticationTrustResolver TRUST_RESOLVER =
            new AuthenticationTrustResolverImpl();
    private static final Set<String> CAMPOS_SENSIBLES =
            Set.of("password", "passwor", "clave", "token", "secret", "hash");

    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioService usuarioService;

    /* ------------------------- API de consulta (solo admin) ------------------------- */

    @Transactional(readOnly = true)
    public Page<AuditoriaResponseDTO> buscar(Long id_usuario, AccionAuditoria accion, String tabla,
                                             LocalDateTime desde, LocalDateTime hasta,
                                             Pageable pageable) {
        validarAdministrador();
        String filtroTabla = tabla == null || tabla.isBlank() ? null : tabla.trim();
        return auditoriaRepository.buscar(id_usuario, accion, filtroTabla, desde, hasta, pageable)
                .map(this::mapearAResponse);
    }

    @Transactional(readOnly = true)
    public AuditoriaResponseDTO obtener(Long id_auditoria) {
        validarAdministrador();
        return auditoriaRepository.findById(id_auditoria)
                .map(this::mapearAResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Auditoría no encontrada con id " + id_auditoria));
    }

    /**
     * Alta manual de una acción de negocio. El usuario y la fecha vienen de la sesión para
     * que el cliente no pueda atribuir la operación a otra persona. Una corrección no modifica
     * la fila original: agrega una nueva que apunta a ella y explica el motivo.
     */
    @Transactional
    public AuditoriaResponseDTO crear(AuditoriaCreateDTO dto) {
        validarAdministrador();
        Actor actor = actorDeLaSesion(true);
        Map<String, Object> cambios = sanearCambios(dto.cambios());

        Long idCorregida = null;
        String tabla = dto.tabla();
        Long idRegistro = dto.id_registro();
        String detalle = dto.detalle();
        if (dto.accion() == AccionAuditoria.CORRECCION_AUDITORIA) {
            idCorregida = validarCorreccion(dto.id_auditoria_corregida(), cambios);
            tabla = "Auditoria";
            idRegistro = null;
            detalle = armarDetalleCorreccion(idCorregida, detalle);
        }

        Auditoria auditoria = new Auditoria(
                LocalDateTime.now(), actor.id(), actor.nombre(), dto.accion(), tabla, idRegistro,
                cambios, detalle, idCorregida);
        return mapearAResponse(auditoriaRepository.save(auditoria));
    }

    private Long validarCorreccion(Long idCorregida, Map<String, Object> cambios) {
        if (idCorregida == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El id de la auditoría a corregir es obligatorio");
        }
        auditoriaRepository.findById(idCorregida)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Auditoría a corregir no encontrada con id " + idCorregida));
        if (cambios.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La corrección debe incluir al menos un campo modificado");
        }
        return idCorregida;
    }

    private static String armarDetalleCorreccion(Long idCorregida, String motivo) {
        String detalle = "Corrige registro #" + idCorregida;
        if (motivo != null && !motivo.isBlank()) {
            detalle = detalle + ": " + motivo.trim();
        }
        return detalle.length() > 255 ? detalle.substring(0, 255) : detalle;
    }

    /* ------------- Registro automático (lo usan los listeners, no la API) ------------- */

    /** Lo llama el listener de JPA después de un insert/update/delete de otra entidad. */
    @Transactional
    public void registrarAutomatico(AccionAuditoria accion, String tabla, Long id_registro,
                                    Map<String, Object> cambios, String detalle) {
        Actor actor = actorDeLaSesion(false);
        auditoriaRepository.save(new Auditoria(
                LocalDateTime.now(), actor.id(), actor.nombre(), accion, tabla, id_registro,
                sanearCambios(cambios), detalle));
    }

    /** Registra un inicio de sesión correcto sin depender del controller de autenticación. */
    @Transactional
    public void registrarLogin(String email) {
        Long id_usuario = null;
        try {
            id_usuario = usuarioService.buscarPorEmail(email).id_usuario();
        } catch (RuntimeException ex) {
            // No se puede impedir el login porque falló la búsqueda del id para auditar.
            id_usuario = null;
        }
        auditoriaRepository.save(new Auditoria(
                LocalDateTime.now(), id_usuario, email, AccionAuditoria.LOGIN, "Usuario",
                id_usuario, Map.of(), "Inicio de sesión"));
    }

    /* --------------------------------- Utilidades --------------------------------- */

    private void validarAdministrador() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()
                || TRUST_RESOLVER.isAnonymous(autenticacion)) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Se requiere iniciar sesión para consultar la auditoría");
        }
        boolean esAdmin = autenticacion.getAuthorities().stream()
                .anyMatch(autoridad -> "ROLE_ADMINISTRADOR".equals(autoridad.getAuthority()));
        if (!esAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo los administradores pueden acceder a la auditoría");
        }
    }

    private Actor actorDeLaSesion(boolean obligatorio) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()
                || TRUST_RESOLVER.isAnonymous(autenticacion)) {
            if (obligatorio) {
                throw new AuthenticationCredentialsNotFoundException(
                        "Se requiere iniciar sesión para registrar la operación");
            }
            return new Actor(null, SISTEMA);
        }

        String nombre = autenticacion.getName();
        try {
            return new Actor(usuarioService.buscarPorEmail(nombre).id_usuario(), nombre);
        } catch (RuntimeException ex) {
            if (obligatorio) {
                throw new RecursoNoEncontradoException("Usuario autenticado no encontrado: " + nombre);
            }
            return new Actor(null, nombre);
        }
    }

    private static Map<String, Object> sanearCambios(Map<String, Object> cambios) {
        if (cambios == null || cambios.isEmpty()) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> resultado = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entrada : cambios.entrySet()) {
            if (!esCampoSensible(entrada.getKey())) {
                resultado.put(entrada.getKey(), sanearValor(entrada.getValue()));
            }
        }
        return resultado;
    }

    private static Object sanearValor(Object valor) {
        if (valor instanceof Map<?, ?> mapa) {
            Map<String, Object> limpio = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entrada : mapa.entrySet()) {
                String clave = String.valueOf(entrada.getKey());
                if (!esCampoSensible(clave)) {
                    limpio.put(clave, sanearValor(entrada.getValue()));
                }
            }
            return limpio;
        }
        if (valor instanceof Collection<?> coleccion) {
            List<Object> limpia = new ArrayList<>(coleccion.size());
            for (Object elemento : coleccion) {
                limpia.add(sanearValor(elemento));
            }
            return limpia;
        }
        return valor;
    }

    public static boolean esCampoSensible(String nombreCampo) {
        if (nombreCampo == null) {
            return false;
        }
        String normalizado = nombreCampo.toLowerCase(Locale.ROOT).replace("_", "");
        return CAMPOS_SENSIBLES.stream().anyMatch(normalizado::contains);
    }

    /**
     * El origen se deduce de la acción: las altas, bajas, modificaciones y los logins los
     * escriben los listeners; el resto son cargas manuales. Así no hace falta migrar la tabla.
     */
    private static OrigenAuditoria origenDe(AccionAuditoria accion) {
        return switch (accion) {
            case ALTA, MODIFICACION, BAJA, LOGIN -> OrigenAuditoria.AUTOMATICO;
            default -> OrigenAuditoria.MANUAL;
        };
    }

    private AuditoriaResponseDTO mapearAResponse(Auditoria auditoria) {
        return new AuditoriaResponseDTO(
                auditoria.getId_auditoria(),
                auditoria.getFecha_hora(),
                auditoria.getId_usuario(),
                auditoria.getUsuario(),
                auditoria.getAccion(),
                origenDe(auditoria.getAccion()),
                auditoria.getTabla(),
                auditoria.getId_registro(),
                auditoria.getId_auditoria_corregida(),
                auditoria.getCambios(),
                auditoria.getDetalle());
    }

    private record Actor(Long id, String nombre) {
    }
}

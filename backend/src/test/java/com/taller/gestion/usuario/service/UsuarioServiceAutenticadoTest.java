package com.taller.gestion.usuario.service;

import com.taller.gestion.common.exception.OperacionNoPermitidaException;
import com.taller.gestion.usuario.entity.Rol;
import com.taller.gestion.usuario.entity.Usuario;
import com.taller.gestion.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceAutenticadoTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    @AfterEach
    void limpiarSeguridad() {
        SecurityContextHolder.clearContext();
    }

    private void autenticar(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "clave",
                        List.of(new SimpleGrantedAuthority("ROLE_VENDEDOR"))));
    }

    private Usuario usuario(boolean activo) {
        Usuario usuario = new Usuario();
        usuario.setId_usuario(7L);
        usuario.setNombre("Operador");
        usuario.setEmail("operador@taller.local");
        usuario.setRol(Rol.VENDEDOR);
        usuario.setActivo(activo);
        return usuario;
    }

    @Test
    @DisplayName("Devuelve el usuario de la sesión buscándolo por email")
    void obtenerAutenticado_devuelveUsuarioPorEmail() {
        autenticar("OPERADOR@taller.local");
        when(usuarioRepository.findByEmailIgnoreCase("OPERADOR@taller.local"))
                .thenReturn(Optional.of(usuario(true)));

        Usuario resultado = usuarioService.obtenerAutenticado();

        assertThat(resultado.getId_usuario()).isEqualTo(7L);
    }

    @Test
    @DisplayName("Rechaza la petición anónima")
    void obtenerAutenticado_sinSesionLanzaExcepcion() {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("clave", "anonymousUser",
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThatThrownBy(() -> usuarioService.obtenerAutenticado())
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    @DisplayName("Rechaza un usuario desactivado aunque tenga sesión abierta")
    void obtenerAutenticado_usuarioInactivoLanzaExcepcion() {
        autenticar("operador@taller.local");
        when(usuarioRepository.findByEmailIgnoreCase("operador@taller.local"))
                .thenReturn(Optional.of(usuario(false)));

        assertThatThrownBy(() -> usuarioService.obtenerAutenticado())
                .isInstanceOf(OperacionNoPermitidaException.class);
    }
}

package com.taller.gestion.auditoria.service;

import com.taller.gestion.auditoria.dto.AuditoriaCreateDTO;
import com.taller.gestion.auditoria.dto.AuditoriaResponseDTO;
import com.taller.gestion.auditoria.entity.AccionAuditoria;
import com.taller.gestion.auditoria.entity.Auditoria;
import com.taller.gestion.auditoria.entity.OrigenAuditoria;
import com.taller.gestion.auditoria.repository.AuditoriaRepository;
import com.taller.gestion.common.exception.RecursoNoEncontradoException;
import com.taller.gestion.usuario.dto.UsuarioResponseDTO;
import com.taller.gestion.usuario.entity.Rol;
import com.taller.gestion.usuario.service.UsuarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditoriaServiceTest {

    @Mock
    private AuditoriaRepository auditoriaRepository;

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private AuditoriaService auditoriaService;

    @AfterEach
    void limpiarSeguridad() {
        SecurityContextHolder.clearContext();
    }

    private void autenticar(String rol) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "ana@taller.local", "clave", List.of(new SimpleGrantedAuthority(rol))));
    }

    @Test
    @DisplayName("El alta manual toma la fecha y el usuario de la sesión")
    void crearConSesionDeAdministrador() {
        autenticar("ROLE_ADMINISTRADOR");
        when(usuarioService.buscarPorEmail("ana@taller.local")).thenReturn(
                new UsuarioResponseDTO(4L, "Ana", "ana@taller.local", Rol.ADMINISTRADOR, true));
        when(auditoriaRepository.save(any(Auditoria.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        AuditoriaResponseDTO resultado = auditoriaService.crear(new AuditoriaCreateDTO(
                AccionAuditoria.CIERRE_CAJA, "Sesion_Caja", 12L,
                Map.of("total_declarado", Map.of("anterior", 100, "nuevo", 95)),
                "Cierre con diferencia"));

        assertThat(resultado.id_usuario()).isEqualTo(4L);
        assertThat(resultado.usuario()).isEqualTo("ana@taller.local");
        assertThat(resultado.accion()).isEqualTo(AccionAuditoria.CIERRE_CAJA);
        assertThat(resultado.origen()).isEqualTo(OrigenAuditoria.MANUAL);
        assertThat(resultado.id_auditoria_corregida()).isNull();
        assertThat(resultado.fecha_hora()).isNotNull();

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository).save(captor.capture());
        assertThat(captor.getValue().getFecha_hora()).isNotNull();
        assertThat(captor.getValue().getCambios()).containsKey("total_declarado");
    }

    @Test
    @DisplayName("Nunca guarda contraseñas dentro del JSON de cambios")
    void crearSaneaCamposSensibles() {
        autenticar("ROLE_ADMINISTRADOR");
        when(usuarioService.buscarPorEmail("ana@taller.local")).thenReturn(
                new UsuarioResponseDTO(4L, "Ana", "ana@taller.local", Rol.ADMINISTRADOR, true));
        when(auditoriaRepository.save(any(Auditoria.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        AuditoriaResponseDTO resultado = auditoriaService.crear(new AuditoriaCreateDTO(
                AccionAuditoria.MODIFICACION, "Usuario", 4L,
                Map.of("password", "$2a$10$hash", "nombre", "Ana"), null));

        assertThat(resultado.cambios()).containsKey("nombre").doesNotContainKey("password");
    }

    @Test
    @DisplayName("La corrección agrega una fila nueva que apunta a la original")
    void correccionConservaLaFilaOriginal() {
        autenticar("ROLE_ADMINISTRADOR");
        when(usuarioService.buscarPorEmail("ana@taller.local")).thenReturn(
                new UsuarioResponseDTO(4L, "Ana", "ana@taller.local", Rol.ADMINISTRADOR, true));
        Auditoria original = new Auditoria(LocalDateTime.now(), 4L, "ana@taller.local",
                AccionAuditoria.OTRO, "Caja", null,
                Map.of("monto", Map.of("anterior", "", "nuevo", 100)), "Carga inicial", null);
        when(auditoriaRepository.findById(123L)).thenReturn(Optional.of(original));
        when(auditoriaRepository.save(any(Auditoria.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        AuditoriaResponseDTO resultado = auditoriaService.crear(new AuditoriaCreateDTO(
                AccionAuditoria.CORRECCION_AUDITORIA, "Auditoria", null,
                Map.of("monto", Map.of("anterior", 100, "nuevo", 95)), "Monto mal cargado", 123L));

        assertThat(resultado.accion()).isEqualTo(AccionAuditoria.CORRECCION_AUDITORIA);
        assertThat(resultado.origen()).isEqualTo(OrigenAuditoria.MANUAL);
        assertThat(resultado.id_auditoria_corregida()).isEqualTo(123L);
        assertThat(resultado.detalle()).isEqualTo("Corrige registro #123: Monto mal cargado");

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository).save(captor.capture());
        assertThat(captor.getValue().getId_auditoria()).isNull();
        assertThat(original.getDetalle()).isEqualTo("Carga inicial");
    }

    @Test
    @DisplayName("Una corrección sin fila destino se rechaza")
    void correccionRequiereDestinoExistente() {
        autenticar("ROLE_ADMINISTRADOR");
        when(usuarioService.buscarPorEmail("ana@taller.local")).thenReturn(
                new UsuarioResponseDTO(4L, "Ana", "ana@taller.local", Rol.ADMINISTRADOR, true));
        when(auditoriaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auditoriaService.crear(new AuditoriaCreateDTO(
                AccionAuditoria.CORRECCION_AUDITORIA, null, null,
                Map.of("monto", Map.of("anterior", 100, "nuevo", 95)), "Motivo", 999L)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("Un vendedor no puede consultar ni registrar auditoría")
    void rechazaVendedor() {
        autenticar("ROLE_VENDEDOR");

        assertThatThrownBy(() -> auditoriaService.obtener(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
        assertThatThrownBy(() -> auditoriaService.crear(new AuditoriaCreateDTO(
                AccionAuditoria.OTRO, null, null, null, null)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("Una petición anónima se rechaza como no autenticada")
    void rechazaAnonimo() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "clave", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThatThrownBy(() -> auditoriaService.obtener(1L))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    @DisplayName("El registro automático sin sesión se atribuye al sistema")
    void registroAutomaticoSinSesion() {
        auditoriaService.registrarAutomatico(AccionAuditoria.ALTA, "Cliente", 7L, Map.of(), null);

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository).save(captor.capture());
        assertThat(captor.getValue().getUsuario()).isEqualTo("SISTEMA");
        assertThat(captor.getValue().getId_usuario()).isNull();
    }
}

package com.taller.gestion.auditoria.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taller.gestion.auditoria.dto.AuditoriaCreateDTO;
import com.taller.gestion.auditoria.dto.AuditoriaResponseDTO;
import com.taller.gestion.auditoria.entity.AccionAuditoria;
import com.taller.gestion.auditoria.entity.OrigenAuditoria;
import com.taller.gestion.auditoria.service.AuditoriaService;
import com.taller.gestion.common.security.ErrorResponseWriter;
import com.taller.gestion.common.security.RestAccessDeniedHandler;
import com.taller.gestion.common.security.RestAuthenticationEntryPoint;
import com.taller.gestion.common.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditoriaController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class, ErrorResponseWriter.class})
@WithMockUser(roles = "ADMINISTRADOR")
class AuditoriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuditoriaService auditoriaService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private AuditoriaResponseDTO registro() {
        return new AuditoriaResponseDTO(1L, LocalDateTime.of(2026, 11, 26, 10, 15),
                4L, "ana@taller.local", AccionAuditoria.CIERRE_CAJA, OrigenAuditoria.MANUAL,
                "Sesion_Caja", 12L, null,
                Map.of("total_declarado", Map.of("anterior", 100, "nuevo", 95)), "Cierre con diferencia");
    }

    @Test
    @DisplayName("Lista con filtros y devuelve el JSON de cambios")
    void listar() throws Exception {
        when(auditoriaService.buscar(eq(4L), eq(AccionAuditoria.CIERRE_CAJA), eq("caja"),
                any(), any(), any())).thenReturn(new PageImpl<>(List.of(registro())));

        mockMvc.perform(get("/api/auditoria")
                        .param("id_usuario", "4")
                        .param("accion", "CIERRE_CAJA")
                        .param("tabla", "caja"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].accion").value("CIERRE_CAJA"))
                .andExpect(jsonPath("$.content[0].origen").value("MANUAL"))
                .andExpect(jsonPath("$.content[0].cambios.total_declarado.nuevo").value(95));

        verify(auditoriaService).buscar(eq(4L), eq(AccionAuditoria.CIERRE_CAJA), eq("caja"),
                any(), any(), any());
    }

    @Test
    @DisplayName("Obtiene un registro por id")
    void obtener() throws Exception {
        when(auditoriaService.obtener(1L)).thenReturn(registro());

        mockMvc.perform(get("/api/auditoria/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id_auditoria").value(1))
                .andExpect(jsonPath("$.usuario").value("ana@taller.local"));
    }

    @Test
    @DisplayName("Da de alta una acción de negocio con 201")
    void crear() throws Exception {
        when(auditoriaService.crear(any(AuditoriaCreateDTO.class))).thenReturn(registro());

        String cuerpo = objectMapper.writeValueAsString(new AuditoriaCreateDTO(
                AccionAuditoria.CIERRE_CAJA, "Sesion_Caja", 12L, Map.of(), "Cierre"));

        mockMvc.perform(post("/api/auditoria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accion").value("CIERRE_CAJA"));

        verify(auditoriaService).crear(any(AuditoriaCreateDTO.class));
    }

    @Test
    @DisplayName("Publica una corrección con el id de la fila que se enmienda")
    void corregir() throws Exception {
        when(auditoriaService.crear(any(AuditoriaCreateDTO.class))).thenReturn(registro());

        String cuerpo = objectMapper.writeValueAsString(new AuditoriaCreateDTO(
                AccionAuditoria.CORRECCION_AUDITORIA, "Auditoria", null,
                Map.of("monto", Map.of("anterior", 100, "nuevo", 95)), "Monto mal cargado", 123L));

        mockMvc.perform(post("/api/auditoria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated());

        ArgumentCaptor<AuditoriaCreateDTO> captor = ArgumentCaptor.forClass(AuditoriaCreateDTO.class);
        verify(auditoriaService).crear(captor.capture());
        assertThat(captor.getValue().accion()).isEqualTo(AccionAuditoria.CORRECCION_AUDITORIA);
        assertThat(captor.getValue().id_auditoria_corregida()).isEqualTo(123L);
    }
}

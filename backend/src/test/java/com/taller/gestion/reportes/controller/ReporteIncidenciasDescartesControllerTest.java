package com.taller.gestion.reportes.controller;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.common.security.ErrorResponseWriter;
import com.taller.gestion.common.security.RestAccessDeniedHandler;
import com.taller.gestion.common.security.RestAuthenticationEntryPoint;
import com.taller.gestion.common.security.SecurityConfig;
import com.taller.gestion.reportes.dto.ConteoIncidenciaPorTipoDTO;
import com.taller.gestion.reportes.dto.IncidenciaDescartesFilaDTO;
import com.taller.gestion.reportes.dto.ReporteIncidenciasDescartesDTO;
import com.taller.gestion.reportes.dto.TipoArticuloStock;
import com.taller.gestion.reportes.dto.TipoIncidenciaOpcionDTO;
import com.taller.gestion.reportes.dto.TotalesIncidenciasDescartesDTO;
import com.taller.gestion.reportes.service.ReporteIncidenciasDescartesService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReporteIncidenciasDescartesController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class, ErrorResponseWriter.class})
@WithMockUser(roles = "ADMINISTRADOR")
class ReporteIncidenciasDescartesControllerTest {

    private static final String URL = "/api/reportes/incidencias-descartes";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReporteIncidenciasDescartesService service;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private ReporteIncidenciasDescartesDTO respuesta() {
        IncidenciaDescartesFilaDTO fila = new IncidenciaDescartesFilaDTO(
                101L, "Rotura", "ABIERTA", TipoArticuloStock.INSUMO, 10L, "Harina", "kg",
                2L, new BigDecimal("3.000"), 2L, 0L, 1L, new BigDecimal("1.000"), true);
        return new ReporteIncidenciasDescartesDTO(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                null, "TODOS", "TODOS", null,
                List.of(new TipoIncidenciaOpcionDTO(101L, "Rotura", true)),
                List.of(fila), new TotalesIncidenciasDescartesDTO(2, 2, 0, 1, 1),
                List.of(new ConteoIncidenciaPorTipoDTO(101L, "Rotura", 2L)),
                List.of("INC-07 pendiente"));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Sin sesión: el reporte devuelve 401")
    void sinSesion_devuelve401() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        verify(service, never()).incidenciasDescartes(any(), any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    @DisplayName("Rol vendedor: el reporte devuelve 403")
    void conRolVendedor_devuelve403() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verify(service, never()).incidenciasDescartes(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Administrador: 200 con la tabla, el gráfico y los avisos")
    void conRolAdministrador_devuelveReporte() throws Exception {
        when(service.incidenciasDescartes(any(), any(), any(), any(), any(), any()))
                .thenReturn(respuesta());

        mockMvc.perform(get(URL)
                        .param("desde", "2026-11-01")
                        .param("hasta", "2026-11-30")
                        .param("estado", "ABIERTA")
                        .param("tipo_articulo", "INSUMO")
                        .param("id_articulo", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filas[0].posibleTraslape").value(true))
                .andExpect(jsonPath("$.filas[0].cantidadDescartada").value(1.0))
                .andExpect(jsonPath("$.totales.incidencias").value(2))
                .andExpect(jsonPath("$.incidenciasPorTipo[0].incidencias").value(2))
                .andExpect(jsonPath("$.avisos[0]").value("INC-07 pendiente"));

        verify(service).incidenciasDescartes(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                null, "ABIERTA", "INSUMO", 10L);
    }

    @Test
    @DisplayName("Fecha con formato inválido o faltante: 400 sin llamar al service")
    void fechasInvalidas_devuelve400() throws Exception {
        mockMvc.perform(get(URL).param("desde", "ayer").param("hasta", "2026-11-30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(
                        "Las fechas deben tener el formato AAAA-MM-DD"));
        mockMvc.perform(get(URL).param("hasta", "2026-11-30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El parámetro 'desde' es obligatorio"));
        verify(service, never()).incidenciasDescartes(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Error de negocio del service: 400 con su mensaje")
    void errorDelService_devuelve400() throws Exception {
        when(service.incidenciasDescartes(any(), any(), any(), any(), any(), any()))
                .thenThrow(new DatosInvalidosException("El rango no puede superar los 5 años"));

        mockMvc.perform(get(URL).param("desde", "2020-01-01").param("hasta", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El rango no puede superar los 5 años"));
    }
}

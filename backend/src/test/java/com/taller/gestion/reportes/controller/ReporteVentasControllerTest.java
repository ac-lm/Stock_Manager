package com.taller.gestion.reportes.controller;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.common.security.ErrorResponseWriter;
import com.taller.gestion.common.security.RestAccessDeniedHandler;
import com.taller.gestion.common.security.RestAuthenticationEntryPoint;
import com.taller.gestion.common.security.SecurityConfig;
import com.taller.gestion.reportes.dto.AgrupacionPeriodo;
import com.taller.gestion.reportes.dto.ReporteVentasPorPeriodoDTO;
import com.taller.gestion.reportes.dto.TotalesVentasPorPeriodoDTO;
import com.taller.gestion.reportes.dto.VentasPorPeriodoFilaDTO;
import com.taller.gestion.reportes.service.ReporteVentasService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReporteVentasController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class, ErrorResponseWriter.class})
@WithMockUser(roles = "ADMINISTRADOR")
class ReporteVentasControllerTest {

    private static final String URL = "/api/reportes/ventas-por-periodo";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReporteVentasService reporteVentasService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private ReporteVentasPorPeriodoDTO respuesta() {
        return new ReporteVentasPorPeriodoDTO(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3), AgrupacionPeriodo.DIA,
                List.of(new VentasPorPeriodoFilaDTO("2026-10-01", new BigDecimal("150.00"), 2,
                                new BigDecimal("75.00")),
                        new VentasPorPeriodoFilaDTO("2026-10-02", new BigDecimal("0.00"), 0,
                                new BigDecimal("0.00"))),
                new TotalesVentasPorPeriodoDTO(new BigDecimal("150.00"), 2,
                        new BigDecimal("75.00")));
    }

    // ============================ SEGURIDAD ============================

    @Test
    @WithAnonymousUser
    @DisplayName("Sin sesión: el reporte devuelve 401")
    void sinSesion_devuelve401() throws Exception {
        mockMvc.perform(get(URL).param("desde", "2026-10-01").param("hasta", "2026-10-31"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        verify(reporteVentasService, never()).ventasPorPeriodo(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    @DisplayName("Rol vendedor: el reporte devuelve 403")
    void conRolVendedor_devuelve403() throws Exception {
        mockMvc.perform(get(URL).param("desde", "2026-10-01").param("hasta", "2026-10-31"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verify(reporteVentasService, never()).ventasPorPeriodo(any(), any(), any());
    }

    // ============================ CONSULTA ============================

    @Test
    @DisplayName("Administrador: 200 con filas y totales separados")
    void conRolAdministrador_devuelveReporte() throws Exception {
        when(reporteVentasService.ventasPorPeriodo(any(), any(), any())).thenReturn(respuesta());

        mockMvc.perform(get(URL).param("desde", "2026-10-01")
                        .param("hasta", "2026-10-03").param("agrupacion", "dia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desde").value("2026-10-01"))
                .andExpect(jsonPath("$.hasta").value("2026-10-03"))
                .andExpect(jsonPath("$.agrupacion").value("dia"))
                .andExpect(jsonPath("$.filas[0].periodo").value("2026-10-01"))
                .andExpect(jsonPath("$.filas[0].totalVendido").value(150.00))
                .andExpect(jsonPath("$.filas[1].cantidadVentas").value(0))
                .andExpect(jsonPath("$.totales.totalVendido").value(150.00))
                .andExpect(jsonPath("$.totales.cantidadVentas").value(2));

        verify(reporteVentasService).ventasPorPeriodo(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3), "dia");
    }

    @Test
    @DisplayName("Sin parámetro de agrupación: el controller usa dia por defecto")
    void sinAgrupacion_usaDia() throws Exception {
        when(reporteVentasService.ventasPorPeriodo(any(), any(), any())).thenReturn(respuesta());

        mockMvc.perform(get(URL).param("desde", "2026-10-01").param("hasta", "2026-10-03"))
                .andExpect(status().isOk());

        verify(reporteVentasService).ventasPorPeriodo(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3), "dia");
    }

    // ============================ VALIDACIÓN ============================

    @Test
    @DisplayName("Fecha con formato inválido: 400 y no llama al service")
    void fechaInvalida_devuelve400() throws Exception {
        mockMvc.perform(get(URL).param("desde", "01-10-2026").param("hasta", "2026-10-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El parámetro 'desde' tiene un formato inválido"));
        verify(reporteVentasService, never()).ventasPorPeriodo(any(), any(), any());
    }

    @Test
    @DisplayName("Sin fechas: 400 y no llama al service")
    void sinFechas_devuelve400() throws Exception {
        when(reporteVentasService.ventasPorPeriodo(any(), any(), any()))
                .thenThrow(new DatosInvalidosException("El rango de fechas es obligatorio"));

        mockMvc.perform(get(URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El rango de fechas es obligatorio"));
        verify(reporteVentasService).ventasPorPeriodo(eq(null), eq(null), eq("dia"));
    }

    @Test
    @DisplayName("Rango invertido del service: 400")
    void rangoInvertido_devuelve400() throws Exception {
        when(reporteVentasService.ventasPorPeriodo(any(), any(), any()))
                .thenThrow(new DatosInvalidosException(
                        "La fecha 'desde' no puede ser posterior a 'hasta'"));

        mockMvc.perform(get(URL).param("desde", "2026-10-31").param("hasta", "2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje")
                        .value("La fecha 'desde' no puede ser posterior a 'hasta'"));
    }
}

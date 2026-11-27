package com.taller.gestion.reportes.controller;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.common.security.ErrorResponseWriter;
import com.taller.gestion.common.security.RestAccessDeniedHandler;
import com.taller.gestion.common.security.RestAuthenticationEntryPoint;
import com.taller.gestion.common.security.SecurityConfig;
import com.taller.gestion.reportes.dto.ReporteVentasPorVendedorDTO;
import com.taller.gestion.reportes.dto.TotalesVentasPorVendedorDTO;
import com.taller.gestion.reportes.dto.VentasPorVendedorFilaDTO;
import com.taller.gestion.reportes.service.ReporteVentasPorVendedorService;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReporteVentasPorVendedorController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class, ErrorResponseWriter.class})
@WithMockUser(roles = "ADMINISTRADOR")
class ReporteVentasPorVendedorControllerTest {

    private static final String URL = "/api/reportes/ventas-por-vendedor";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReporteVentasPorVendedorService reporteVentasPorVendedorService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private ReporteVentasPorVendedorDTO respuesta() {
        VentasPorVendedorFilaDTO fila = new VentasPorVendedorFilaDTO(
                7L, "Ana Pérez (VENDEDOR)", 3, new BigDecimal("150.00"),
                new BigDecimal("50.00"));
        return new ReporteVentasPorVendedorDTO(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3), 7L, List.of(fila),
                new TotalesVentasPorVendedorDTO(
                        3, new BigDecimal("150.00"), new BigDecimal("50.00"), 1));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Sin sesión: el reporte devuelve 401")
    void sinSesion_devuelve401() throws Exception {
        mockMvc.perform(get(URL).param("desde", "2026-10-01").param("hasta", "2026-10-31"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        verify(reporteVentasPorVendedorService, never()).ventasPorVendedor(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    @DisplayName("Rol vendedor: el reporte devuelve 403")
    void conRolVendedor_devuelve403() throws Exception {
        mockMvc.perform(get(URL).param("desde", "2026-10-01").param("hasta", "2026-10-31"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verify(reporteVentasPorVendedorService, never()).ventasPorVendedor(any(), any(), any());
    }

    @Test
    @DisplayName("Administrador: 200 con filas y totales")
    void conRolAdministrador_devuelveReporte() throws Exception {
        when(reporteVentasPorVendedorService.ventasPorVendedor(any(), any(), any()))
                .thenReturn(respuesta());

        mockMvc.perform(get(URL)
                        .param("desde", "2026-10-01").param("hasta", "2026-10-03")
                        .param("id_vendedor", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desde").value("2026-10-01"))
                .andExpect(jsonPath("$.hasta").value("2026-10-03"))
                .andExpect(jsonPath("$.id_vendedor").value(7))
                .andExpect(jsonPath("$.filas[0].vendedor").value("Ana Pérez (VENDEDOR)"))
                .andExpect(jsonPath("$.filas[0].cantidadVentas").value(3))
                .andExpect(jsonPath("$.filas[0].ticketPromedio").value(50.00))
                .andExpect(jsonPath("$.totales.totalVendido").value(150.00));

        verify(reporteVentasPorVendedorService).ventasPorVendedor(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3), 7L);
    }

    @Test
    @DisplayName("Filtro opcional ausente: sin vendedor")
    void sinFiltroOpcional() throws Exception {
        when(reporteVentasPorVendedorService.ventasPorVendedor(any(), any(), any()))
                .thenReturn(respuesta());

        mockMvc.perform(get(URL).param("desde", "2026-10-01").param("hasta", "2026-10-03"))
                .andExpect(status().isOk());

        verify(reporteVentasPorVendedorService).ventasPorVendedor(
                eq(LocalDate.of(2026, 10, 1)), eq(LocalDate.of(2026, 10, 3)), isNull());
    }

    @Test
    @DisplayName("Fecha con formato inválido: 400 y no llama al service")
    void fechaInvalida_devuelve400() throws Exception {
        mockMvc.perform(get(URL).param("desde", "01-10-2026").param("hasta", "2026-10-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El parámetro 'desde' tiene un formato inválido"));
        verify(reporteVentasPorVendedorService, never()).ventasPorVendedor(any(), any(), any());
    }

    @Test
    @DisplayName("Vendedor con formato inválido: 400 y no llama al service")
    void vendedorInvalido_devuelve400() throws Exception {
        mockMvc.perform(get(URL).param("desde", "2026-10-01").param("hasta", "2026-10-31")
                        .param("id_vendedor", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje")
                        .value("El parámetro 'id_vendedor' tiene un formato inválido"));
        verify(reporteVentasPorVendedorService, never()).ventasPorVendedor(any(), any(), any());
    }

    @Test
    @DisplayName("Error de negocio del service: 400 con su mensaje")
    void errorDelService_devuelve400() throws Exception {
        when(reporteVentasPorVendedorService.ventasPorVendedor(any(), any(), any()))
                .thenThrow(new DatosInvalidosException("El rango de fechas es obligatorio"));

        mockMvc.perform(get(URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El rango de fechas es obligatorio"));
    }
}

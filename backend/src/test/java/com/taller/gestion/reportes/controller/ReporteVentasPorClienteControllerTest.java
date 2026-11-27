package com.taller.gestion.reportes.controller;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.common.security.ErrorResponseWriter;
import com.taller.gestion.common.security.RestAccessDeniedHandler;
import com.taller.gestion.common.security.RestAuthenticationEntryPoint;
import com.taller.gestion.common.security.SecurityConfig;
import com.taller.gestion.reportes.dto.ReporteVentasPorClienteDTO;
import com.taller.gestion.reportes.dto.TotalesPorTipoClienteDTO;
import com.taller.gestion.reportes.dto.TotalesVentasPorClienteDTO;
import com.taller.gestion.reportes.dto.VentasPorClienteFilaDTO;
import com.taller.gestion.reportes.service.ReporteVentasPorClienteService;
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

@WebMvcTest(ReporteVentasPorClienteController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class, ErrorResponseWriter.class})
@WithMockUser(roles = "ADMINISTRADOR")
class ReporteVentasPorClienteControllerTest {

    private static final String URL = "/api/reportes/ventas-por-cliente";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReporteVentasPorClienteService reporteVentasPorClienteService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private ReporteVentasPorClienteDTO respuesta() {
        VentasPorClienteFilaDTO fila = new VentasPorClienteFilaDTO(
                7L, "Panadería Sol", "GRAN_CLIENTE", "Gran cliente", 3,
                new BigDecimal("150.00"));
        return new ReporteVentasPorClienteDTO(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3), 7L, "GRAN_CLIENTE",
                List.of(fila),
                List.of(new TotalesPorTipoClienteDTO("GRAN_CLIENTE", "Gran cliente", 3,
                                new BigDecimal("150.00")),
                        new TotalesPorTipoClienteDTO("CONSUMIDOR_FINAL", "Consumidor final", 0,
                                new BigDecimal("0.00"))),
                new TotalesVentasPorClienteDTO(3, new BigDecimal("150.00"), 1));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Sin sesión: el reporte devuelve 401")
    void sinSesion_devuelve401() throws Exception {
        mockMvc.perform(get(URL).param("desde", "2026-10-01").param("hasta", "2026-10-31"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        verify(reporteVentasPorClienteService, never()).ventasPorCliente(
                any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    @DisplayName("Rol vendedor: el reporte devuelve 403")
    void conRolVendedor_devuelve403() throws Exception {
        mockMvc.perform(get(URL).param("desde", "2026-10-01").param("hasta", "2026-10-31"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verify(reporteVentasPorClienteService, never()).ventasPorCliente(
                any(), any(), any(), any());
    }

    @Test
    @DisplayName("Administrador: 200 con filas, resumen por tipo y totales")
    void conRolAdministrador_devuelveReporte() throws Exception {
        when(reporteVentasPorClienteService.ventasPorCliente(any(), any(), any(), any()))
                .thenReturn(respuesta());

        mockMvc.perform(get(URL)
                        .param("desde", "2026-10-01").param("hasta", "2026-10-03")
                        .param("id_cliente", "7").param("tipo_cliente", "GRAN_CLIENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desde").value("2026-10-01"))
                .andExpect(jsonPath("$.hasta").value("2026-10-03"))
                .andExpect(jsonPath("$.id_cliente").value(7))
                .andExpect(jsonPath("$.tipo_cliente").value("GRAN_CLIENTE"))
                .andExpect(jsonPath("$.filas[0].cliente").value("Panadería Sol"))
                .andExpect(jsonPath("$.filas[0].cantidadCompras").value(3))
                .andExpect(jsonPath("$.porTipo[1].tipo").value("Consumidor final"))
                .andExpect(jsonPath("$.totales.totalVendido").value(150.00));

        verify(reporteVentasPorClienteService).ventasPorCliente(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3), 7L, "GRAN_CLIENTE");
    }

    @Test
    @DisplayName("Filtros opcionales ausentes: sin cliente ni tipo")
    void sinFiltrosOpcionales() throws Exception {
        when(reporteVentasPorClienteService.ventasPorCliente(any(), any(), any(), any()))
                .thenReturn(respuesta());

        mockMvc.perform(get(URL).param("desde", "2026-10-01").param("hasta", "2026-10-03"))
                .andExpect(status().isOk());

        verify(reporteVentasPorClienteService).ventasPorCliente(
                eq(LocalDate.of(2026, 10, 1)), eq(LocalDate.of(2026, 10, 3)),
                isNull(), isNull());
    }

    @Test
    @DisplayName("Fecha con formato inválido: 400 y no llama al service")
    void fechaInvalida_devuelve400() throws Exception {
        mockMvc.perform(get(URL).param("desde", "01-10-2026").param("hasta", "2026-10-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El parámetro 'desde' tiene un formato inválido"));
        verify(reporteVentasPorClienteService, never()).ventasPorCliente(
                any(), any(), any(), any());
    }

    @Test
    @DisplayName("Cliente con formato inválido: 400 y no llama al service")
    void clienteInvalido_devuelve400() throws Exception {
        mockMvc.perform(get(URL).param("desde", "2026-10-01").param("hasta", "2026-10-31")
                        .param("id_cliente", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje")
                        .value("El parámetro 'id_cliente' tiene un formato inválido"));
        verify(reporteVentasPorClienteService, never()).ventasPorCliente(
                any(), any(), any(), any());
    }

    @Test
    @DisplayName("Error de negocio del service: 400 con su mensaje")
    void errorDelService_devuelve400() throws Exception {
        when(reporteVentasPorClienteService.ventasPorCliente(any(), any(), any(), any()))
                .thenThrow(new DatosInvalidosException("El rango de fechas es obligatorio"));

        mockMvc.perform(get(URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El rango de fechas es obligatorio"));
    }
}

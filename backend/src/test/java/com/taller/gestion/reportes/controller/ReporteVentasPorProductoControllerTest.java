package com.taller.gestion.reportes.controller;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.common.security.ErrorResponseWriter;
import com.taller.gestion.common.security.RestAccessDeniedHandler;
import com.taller.gestion.common.security.RestAuthenticationEntryPoint;
import com.taller.gestion.common.security.SecurityConfig;
import com.taller.gestion.reportes.dto.ReporteVentasPorProductoDTO;
import com.taller.gestion.reportes.dto.TotalesVentasPorProductoDTO;
import com.taller.gestion.reportes.dto.VentasPorProductoFilaDTO;
import com.taller.gestion.reportes.service.ReporteVentasPorProductoService;
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

@WebMvcTest(ReporteVentasPorProductoController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class, ErrorResponseWriter.class})
@WithMockUser(roles = "ADMINISTRADOR")
class ReporteVentasPorProductoControllerTest {

    private static final String URL = "/api/reportes/ventas-por-producto";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReporteVentasPorProductoService reporteVentasPorProductoService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private ReporteVentasPorProductoDTO respuesta() {
        return new ReporteVentasPorProductoDTO(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3), 7L, "Bebidas", 1,
                List.of(new VentasPorProductoFilaDTO(7L, "Café", "Bebidas",
                        new BigDecimal("3.000"), new BigDecimal("150.00"))),
                List.of(new VentasPorProductoFilaDTO(7L, "Café", "Bebidas",
                        new BigDecimal("3.000"), new BigDecimal("150.00"))),
                new TotalesVentasPorProductoDTO(new BigDecimal("3.000"),
                        new BigDecimal("150.00"), 1));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Sin sesión: el reporte devuelve 401")
    void sinSesion_devuelve401() throws Exception {
        mockMvc.perform(get(URL).param("desde", "2026-10-01").param("hasta", "2026-10-31"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        verify(reporteVentasPorProductoService, never()).ventasPorProducto(
                any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    @DisplayName("Rol vendedor: el reporte devuelve 403")
    void conRolVendedor_devuelve403() throws Exception {
        mockMvc.perform(get(URL).param("desde", "2026-10-01").param("hasta", "2026-10-31"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verify(reporteVentasPorProductoService, never()).ventasPorProducto(
                any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Administrador: 200 con filas, ranking y totales")
    void conRolAdministrador_devuelveReporte() throws Exception {
        when(reporteVentasPorProductoService.ventasPorProducto(any(), any(), any(), any(), any()))
                .thenReturn(respuesta());

        mockMvc.perform(get(URL)
                        .param("desde", "2026-10-01").param("hasta", "2026-10-03")
                        .param("id_producto", "7").param("categoria", "Bebidas").param("top", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desde").value("2026-10-01"))
                .andExpect(jsonPath("$.hasta").value("2026-10-03"))
                .andExpect(jsonPath("$.id_producto").value(7))
                .andExpect(jsonPath("$.categoria").value("Bebidas"))
                .andExpect(jsonPath("$.top").value(1))
                .andExpect(jsonPath("$.filas[0].producto").value("Café"))
                .andExpect(jsonPath("$.ranking[0].totalVendido").value(150.00))
                .andExpect(jsonPath("$.totales.cantidadVendida").value(3.000))
                .andExpect(jsonPath("$.totales.productos").value(1));

        verify(reporteVentasPorProductoService).ventasPorProducto(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3), 7L, "Bebidas", 1);
    }

    @Test
    @DisplayName("Filtros opcionales ausentes: top por defecto 10 y sin producto/categoría")
    void sinFiltrosOpcionales_usaTopPorDefecto() throws Exception {
        when(reporteVentasPorProductoService.ventasPorProducto(any(), any(), any(), any(), any()))
                .thenReturn(respuesta());

        mockMvc.perform(get(URL).param("desde", "2026-10-01").param("hasta", "2026-10-03"))
                .andExpect(status().isOk());

        verify(reporteVentasPorProductoService).ventasPorProducto(
                eq(LocalDate.of(2026, 10, 1)), eq(LocalDate.of(2026, 10, 3)),
                isNull(), isNull(), eq(10));
    }

    @Test
    @DisplayName("Fecha con formato inválido: 400 y no llama al service")
    void fechaInvalida_devuelve400() throws Exception {
        mockMvc.perform(get(URL).param("desde", "01-10-2026").param("hasta", "2026-10-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El parámetro 'desde' tiene un formato inválido"));
        verify(reporteVentasPorProductoService, never()).ventasPorProducto(
                any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Producto con formato inválido: 400 y no llama al service")
    void productoInvalido_devuelve400() throws Exception {
        mockMvc.perform(get(URL).param("desde", "2026-10-01").param("hasta", "2026-10-31")
                        .param("id_producto", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje")
                        .value("El parámetro 'id_producto' tiene un formato inválido"));
        verify(reporteVentasPorProductoService, never()).ventasPorProducto(
                any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Top con formato inválido: 400 y no llama al service")
    void topInvalido_devuelve400() throws Exception {
        mockMvc.perform(get(URL).param("desde", "2026-10-01").param("hasta", "2026-10-31")
                        .param("top", "muchos"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El parámetro 'top' debe ser un entero positivo"));
        verify(reporteVentasPorProductoService, never()).ventasPorProducto(
                any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Error de negocio del service: 400 con su mensaje")
    void errorDelService_devuelve400() throws Exception {
        when(reporteVentasPorProductoService.ventasPorProducto(any(), any(), any(), any(), any()))
                .thenThrow(new DatosInvalidosException("El rango de fechas es obligatorio"));

        mockMvc.perform(get(URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El rango de fechas es obligatorio"));
    }
}

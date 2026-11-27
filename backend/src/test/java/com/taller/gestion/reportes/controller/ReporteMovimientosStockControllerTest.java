package com.taller.gestion.reportes.controller;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.common.security.ErrorResponseWriter;
import com.taller.gestion.common.security.RestAccessDeniedHandler;
import com.taller.gestion.common.security.RestAuthenticationEntryPoint;
import com.taller.gestion.common.security.SecurityConfig;
import com.taller.gestion.reportes.dto.MovimientoStockFilaDTO;
import com.taller.gestion.reportes.dto.ReporteMovimientosStockDTO;
import com.taller.gestion.reportes.dto.TipoArticuloStock;
import com.taller.gestion.reportes.dto.TotalesMovimientosStockDTO;
import com.taller.gestion.reportes.service.ReporteMovimientosStockService;
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

@WebMvcTest(ReporteMovimientosStockController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class, ErrorResponseWriter.class})
@WithMockUser(roles = "ADMINISTRADOR")
class ReporteMovimientosStockControllerTest {

    private static final String URL = "/api/reportes/movimientos-stock";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReporteMovimientosStockService service;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private ReporteMovimientosStockDTO respuesta() {
        MovimientoStockFilaDTO fila = new MovimientoStockFilaDTO(
                10L, TipoArticuloStock.INSUMO, "Harina", "kg",
                new BigDecimal("4.000"), BigDecimal.ZERO.setScale(3),
                new BigDecimal("1.000"), BigDecimal.ZERO.setScale(3),
                BigDecimal.ZERO.setScale(3));
        return new ReporteMovimientosStockDTO(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                "INSUMO", 10L, "SALIDA", null,
                List.of(fila), new TotalesMovimientosStockDTO(1, 2));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Sin sesión: el reporte devuelve 401")
    void sinSesion_devuelve401() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        verify(service, never()).movimientosStock(any(), any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    @DisplayName("Rol vendedor: el reporte devuelve 403")
    void conRolVendedor_devuelve403() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verify(service, never()).movimientosStock(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Administrador: 200 con las cinco columnas de cantidades")
    void conRolAdministrador_devuelveReporte() throws Exception {
        when(service.movimientosStock(any(), any(), any(), any(), any(), any()))
                .thenReturn(respuesta());

        mockMvc.perform(get(URL)
                        .param("desde", "2026-11-01")
                        .param("hasta", "2026-11-30")
                        .param("tipo_articulo", "INSUMO")
                        .param("id_articulo", "10")
                        .param("tipo_movimiento", "SALIDA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desde").value("2026-11-01"))
                .andExpect(jsonPath("$.hasta").value("2026-11-30"))
                .andExpect(jsonPath("$.idArticulo").value(10))
                .andExpect(jsonPath("$.filas[0].articulo").value("Harina"))
                .andExpect(jsonPath("$.filas[0].otrasSalidas").value(0))
                .andExpect(jsonPath("$.totales.movimientos").value(2));

        verify(service).movimientosStock(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                "INSUMO", 10L, "SALIDA", null);
    }

    @Test
    @DisplayName("Sin rango: el error de negocio del service se responde como 400")
    void sinRango_devuelve400() throws Exception {
        when(service.movimientosStock(any(), any(), any(), any(), any(), any()))
                .thenThrow(new DatosInvalidosException("El rango de fechas es obligatorio"));

        mockMvc.perform(get(URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El rango de fechas es obligatorio"));
        verify(service).movimientosStock(null, null, null, null, null, null);
    }

    @Test
    @DisplayName("Fecha o id con formato inválido: 400")
    void parametrosInvalidos_devuelve400() throws Exception {
        mockMvc.perform(get(URL).param("desde", "ayer").param("hasta", "2026-11-30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(
                        "El parámetro 'desde' tiene un formato inválido"));

        mockMvc.perform(get(URL).param("desde", "2026-11-01")
                        .param("hasta", "2026-11-30").param("id_proveedor", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(
                        "El parámetro 'id_proveedor' tiene un formato inválido"));
        verify(service, never()).movimientosStock(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Error de negocio del service: 400 con su mensaje")
    void errorDelService_devuelve400() throws Exception {
        when(service.movimientosStock(any(), any(), any(), any(), any(), any()))
                .thenThrow(new DatosInvalidosException("El rango de fechas es obligatorio"));

        mockMvc.perform(get(URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El rango de fechas es obligatorio"));
    }
}

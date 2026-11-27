package com.taller.gestion.reportes.controller;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.common.security.ErrorResponseWriter;
import com.taller.gestion.common.security.RestAccessDeniedHandler;
import com.taller.gestion.common.security.RestAuthenticationEntryPoint;
import com.taller.gestion.common.security.SecurityConfig;
import com.taller.gestion.reportes.dto.ReporteStockActualDTO;
import com.taller.gestion.reportes.dto.StockActualFilaDTO;
import com.taller.gestion.reportes.dto.TipoArticuloStock;
import com.taller.gestion.reportes.dto.TotalesStockActualDTO;
import com.taller.gestion.reportes.service.ReporteStockActualService;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReporteStockActualController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class, ErrorResponseWriter.class})
@WithMockUser(roles = "ADMINISTRADOR")
class ReporteStockActualControllerTest {

    private static final String URL = "/api/reportes/stock-actual";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReporteStockActualService service;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private ReporteStockActualDTO respuesta() {
        StockActualFilaDTO fila = new StockActualFilaDTO(
                10L, TipoArticuloStock.INSUMO, "Harina", null, "kg",
                new BigDecimal("2.000"), 5, new BigDecimal("3.000"), true);
        return new ReporteStockActualDTO(
                LocalDate.of(2026, 11, 26), LocalDate.of(2026, 11, 26), "INSUMO", null,
                true, List.of(fila), List.of(fila), new TotalesStockActualDTO(1, 1));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Sin sesión: el reporte devuelve 401")
    void sinSesion_devuelve401() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        verify(service, never()).stockActual(any(), any(), anyBoolean());
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    @DisplayName("Rol vendedor: el reporte devuelve 403")
    void conRolVendedor_devuelve403() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verify(service, never()).stockActual(any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("Administrador: 200 con filas, gráfico y sin fechas en la URL")
    void conRolAdministrador_devuelveReporte() throws Exception {
        when(service.stockActual(any(), any(), anyBoolean())).thenReturn(respuesta());

        mockMvc.perform(get(URL)
                        .param("tipo", "INSUMO")
                        .param("solo_bajo_minimo", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desde").value("2026-11-26"))
                .andExpect(jsonPath("$.hasta").value("2026-11-26"))
                .andExpect(jsonPath("$.tipo").value("INSUMO"))
                .andExpect(jsonPath("$.soloBajoMinimo").value(true))
                .andExpect(jsonPath("$.filas[0].articulo").value("Harina"))
                .andExpect(jsonPath("$.filas[0].bajoMinimo").value(true))
                .andExpect(jsonPath("$.bajoMinimo[0].falta").value(3.000))
                .andExpect(jsonPath("$.totales.articulos").value(1));

        verify(service).stockActual("INSUMO", null, true);
    }

    @Test
    @DisplayName("Filtros ausentes: todos los tipos y sin acotar a bajo mínimo")
    void sinFiltros() throws Exception {
        when(service.stockActual(any(), any(), anyBoolean())).thenReturn(respuesta());

        mockMvc.perform(get(URL))
                .andExpect(status().isOk());

        verify(service).stockActual(isNull(), isNull(), eq(false));
    }

    @Test
    @DisplayName("Booleano inválido: 400 y no llama al service")
    void booleanoInvalido_devuelve400() throws Exception {
        mockMvc.perform(get(URL).param("solo_bajo_minimo", "quizas"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(
                        "El parámetro 'solo_bajo_minimo' tiene un formato inválido"));
        verify(service, never()).stockActual(any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("Error de negocio del service: 400 con su mensaje")
    void errorDelService_devuelve400() throws Exception {
        when(service.stockActual(any(), any(), anyBoolean()))
                .thenThrow(new DatosInvalidosException("El parámetro 'tipo' tiene un valor inválido"));

        mockMvc.perform(get(URL).param("tipo", "BEBIDA"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El parámetro 'tipo' tiene un valor inválido"));
        verify(service).stockActual(eq("BEBIDA"), isNull(), eq(false));
    }
}

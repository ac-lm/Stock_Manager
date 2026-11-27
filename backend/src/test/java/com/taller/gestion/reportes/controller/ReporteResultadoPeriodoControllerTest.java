package com.taller.gestion.reportes.controller;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.common.security.ErrorResponseWriter;
import com.taller.gestion.common.security.RestAccessDeniedHandler;
import com.taller.gestion.common.security.RestAuthenticationEntryPoint;
import com.taller.gestion.common.security.SecurityConfig;
import com.taller.gestion.reportes.dto.ReporteResultadoPeriodoDTO;
import com.taller.gestion.reportes.dto.ResultadoPeriodoFilaDTO;
import com.taller.gestion.reportes.dto.TotalesResultadoPeriodoDTO;
import com.taller.gestion.reportes.service.ReporteCostosPorTipoService;
import com.taller.gestion.reportes.service.ReporteResultadoPeriodoService;
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
import java.time.YearMonth;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReporteResultadoPeriodoController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class, ErrorResponseWriter.class})
@WithMockUser(roles = "ADMINISTRADOR")
class ReporteResultadoPeriodoControllerTest {

    private static final String URL = "/api/reportes/resultado-periodo";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReporteResultadoPeriodoService service;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private ReporteResultadoPeriodoDTO respuesta() {
        return new ReporteResultadoPeriodoDTO(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 30),
                "2026-10", "2026-11",
                List.of(new ResultadoPeriodoFilaDTO(
                        "2026-10", "oct 2026", new BigDecimal("750.00"),
                        new BigDecimal("200.00"), new BigDecimal("550.00"))),
                new TotalesResultadoPeriodoDTO(
                        new BigDecimal("750.00"), new BigDecimal("200.00"),
                        new BigDecimal("550.00"), 2),
                ReporteResultadoPeriodoService.ACLARACION,
                ReporteCostosPorTipoService.AVISO_INSUMOS);
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Sin sesión: el reporte devuelve 401")
    void sinSesion_devuelve401() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        verify(service, never()).resultadoPeriodo(any(), any());
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    @DisplayName("Rol vendedor: el reporte devuelve 403")
    void conRolVendedor_devuelve403() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verify(service, never()).resultadoPeriodo(any(), any());
    }

    @Test
    @DisplayName("Administrador: 200 con la tabla de ventas, costos y resultado")
    void conRolAdministrador_devuelveReporte() throws Exception {
        when(service.resultadoPeriodo(any(), any())).thenReturn(respuesta());

        mockMvc.perform(get(URL)
                        .param("desde", "2026-10")
                        .param("hasta", "2026-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desdePeriodo").value("2026-10"))
                .andExpect(jsonPath("$.hastaPeriodo").value("2026-11"))
                .andExpect(jsonPath("$.filas[0].totalVendido").value(750.00))
                .andExpect(jsonPath("$.filas[0].totalCostos").value(200.00))
                .andExpect(jsonPath("$.filas[0].resultado").value(550.00))
                .andExpect(jsonPath("$.totales.resultado").value(550.00))
                .andExpect(jsonPath("$.aclaracion").value(
                        ReporteResultadoPeriodoService.ACLARACION))
                .andExpect(jsonPath("$.avisoInsumos").value(
                        ReporteCostosPorTipoService.AVISO_INSUMOS));

        verify(service).resultadoPeriodo(YearMonth.of(2026, 10), YearMonth.of(2026, 11));
    }

    @Test
    @DisplayName("Período con formato inválido: 400")
    void parametrosInvalidos_devuelve400() throws Exception {
        mockMvc.perform(get(URL).param("desde", "octubre").param("hasta", "2026-11"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(
                        "El parámetro 'desde' tiene un formato inválido"));
        verify(service, never()).resultadoPeriodo(any(), any());
    }

    @Test
    @DisplayName("Sin rango o error de negocio: 400 con el mensaje del service")
    void errorDelService_devuelve400() throws Exception {
        when(service.resultadoPeriodo(any(), any()))
                .thenThrow(new DatosInvalidosException("El rango de períodos es obligatorio"));

        mockMvc.perform(get(URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El rango de períodos es obligatorio"));
        verify(service).resultadoPeriodo(null, null);
    }
}

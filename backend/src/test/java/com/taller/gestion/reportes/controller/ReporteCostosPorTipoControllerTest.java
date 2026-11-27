package com.taller.gestion.reportes.controller;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.common.security.ErrorResponseWriter;
import com.taller.gestion.common.security.RestAccessDeniedHandler;
import com.taller.gestion.common.security.RestAuthenticationEntryPoint;
import com.taller.gestion.common.security.SecurityConfig;
import com.taller.gestion.reportes.dto.CostoPorTipoFilaDTO;
import com.taller.gestion.reportes.dto.ReporteCostosPorTipoDTO;
import com.taller.gestion.reportes.dto.TipoCostoOpcionDTO;
import com.taller.gestion.reportes.dto.TipoCostoReporteDTO;
import com.taller.gestion.reportes.dto.TotalesCostosPorTipoDTO;
import com.taller.gestion.reportes.service.ReporteCostosPorTipoService;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReporteCostosPorTipoController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class, ErrorResponseWriter.class})
@WithMockUser(roles = "ADMINISTRADOR")
class ReporteCostosPorTipoControllerTest {

    private static final String URL = "/api/reportes/costos-por-tipo";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReporteCostosPorTipoService service;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private ReporteCostosPorTipoDTO respuesta() {
        TipoCostoOpcionDTO opcion = new TipoCostoOpcionDTO(3L, "Alquiler", true, false);
        TipoCostoReporteDTO tipo = new TipoCostoReporteDTO(
                3L, "Alquiler", false, new BigDecimal("150.00"));
        CostoPorTipoFilaDTO fila = new CostoPorTipoFilaDTO(
                "2026-10", "oct 2026", Map.of("3", new BigDecimal("150.00")),
                new BigDecimal("150.00"));
        return new ReporteCostosPorTipoDTO(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 30),
                "2026-10", "2026-11", null,
                List.of(opcion), List.of(tipo), List.of(fila),
                new TotalesCostosPorTipoDTO(new BigDecimal("150.00"), 2, 1, 1),
                ReporteCostosPorTipoService.AVISO_INSUMOS);
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Sin sesión: el reporte devuelve 401")
    void sinSesion_devuelve401() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        verify(service, never()).costosPorTipo(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    @DisplayName("Rol vendedor: el reporte devuelve 403")
    void conRolVendedor_devuelve403() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verify(service, never()).costosPorTipo(any(), any(), any());
    }

    @Test
    @DisplayName("Administrador: 200 con la tabla por mes y tipo")
    void conRolAdministrador_devuelveReporte() throws Exception {
        when(service.costosPorTipo(any(), any(), any())).thenReturn(respuesta());

        mockMvc.perform(get(URL)
                        .param("desde", "2026-10")
                        .param("hasta", "2026-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desdePeriodo").value("2026-10"))
                .andExpect(jsonPath("$.hastaPeriodo").value("2026-11"))
                .andExpect(jsonPath("$.filas[0].periodo").value("2026-10"))
                .andExpect(jsonPath("$.filas[0].valoresPorTipo['3']").value(150.00))
                .andExpect(jsonPath("$.totales.meses").value(2));

        verify(service).costosPorTipo(
                YearMonth.of(2026, 10), YearMonth.of(2026, 11), null);
    }

    @Test
    @DisplayName("Período con formato inválido o tipo no numérico: 400")
    void parametrosInvalidos_devuelve400() throws Exception {
        mockMvc.perform(get(URL).param("desde", "octubre").param("hasta", "2026-11"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(
                        "El parámetro 'desde' tiene un formato inválido"));

        mockMvc.perform(get(URL).param("desde", "2026-10")
                        .param("hasta", "2026-11").param("id_tipo_costo", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(
                        "El parámetro 'id_tipo_costo' tiene un formato inválido"));
        verify(service, never()).costosPorTipo(any(), any(), any());
    }

    @Test
    @DisplayName("Sin rango o error de negocio: 400 con el mensaje del service")
    void errorDelService_devuelve400() throws Exception {
        when(service.costosPorTipo(any(), any(), any()))
                .thenThrow(new DatosInvalidosException("El rango de períodos es obligatorio"));

        mockMvc.perform(get(URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El rango de períodos es obligatorio"));
        verify(service).costosPorTipo(null, null, null);
    }
}

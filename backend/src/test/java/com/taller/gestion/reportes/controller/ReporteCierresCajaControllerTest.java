package com.taller.gestion.reportes.controller;

import com.taller.gestion.caja.entity.EstadoSesionCaja;
import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.common.security.ErrorResponseWriter;
import com.taller.gestion.common.security.RestAccessDeniedHandler;
import com.taller.gestion.common.security.RestAuthenticationEntryPoint;
import com.taller.gestion.common.security.SecurityConfig;
import com.taller.gestion.reportes.dto.CierreCajaFilaDTO;
import com.taller.gestion.reportes.dto.ReporteCierresCajaDTO;
import com.taller.gestion.reportes.dto.TotalesCierresCajaDTO;
import com.taller.gestion.reportes.dto.ValoresMedioPagoCierreDTO;
import com.taller.gestion.reportes.service.ReporteCierresCajaService;
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
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReporteCierresCajaController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class, ErrorResponseWriter.class})
@WithMockUser(roles = "ADMINISTRADOR")
class ReporteCierresCajaControllerTest {

    private static final String URL = "/api/reportes/cierres-caja";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReporteCierresCajaService service;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private ReporteCierresCajaDTO respuesta() {
        ValoresMedioPagoCierreDTO valores = new ValoresMedioPagoCierreDTO(
                new BigDecimal("100.00"), new BigDecimal("95.00"), new BigDecimal("-5.00"),
                new BigDecimal("100.00"), new BigDecimal("5.00"),
                new BigDecimal("5.00"), BigDecimal.ZERO.setScale(2));
        CierreCajaFilaDTO fila = new CierreCajaFilaDTO(
                12L, LocalDate.of(2026, 10, 1), LocalTime.of(9, 0),
                LocalDate.of(2026, 10, 1), LocalTime.of(17, 0),
                "Ana", "Luis", new BigDecimal("50.00"), EstadoSesionCaja.CERRADA,
                false, Map.of("EFECTIVO", valores));
        return new ReporteCierresCajaDTO(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3), 7L, "TODOS", "EFECTIVO",
                List.of("EFECTIVO"), List.of(fila),
                new TotalesCierresCajaDTO(1, Map.of("EFECTIVO", valores)));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Sin sesión: el reporte devuelve 401")
    void sinSesion_devuelve401() throws Exception {
        mockMvc.perform(get(URL).param("desde", "2026-10-01").param("hasta", "2026-10-31"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        verify(service, never()).cierresCaja(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    @DisplayName("Rol vendedor: el reporte devuelve 403")
    void conRolVendedor_devuelve403() throws Exception {
        mockMvc.perform(get(URL).param("desde", "2026-10-01").param("hasta", "2026-10-31"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verify(service, never()).cierresCaja(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Administrador: 200 con filas, totales y filtros")
    void conRolAdministrador_devuelveReporte() throws Exception {
        when(service.cierresCaja(any(), any(), any(), any(), any())).thenReturn(respuesta());

        mockMvc.perform(get(URL)
                        .param("desde", "2026-10-01").param("hasta", "2026-10-03")
                        .param("id_usuario_cierre", "7")
                        .param("estado", "TODOS").param("medio_pago", "EFECTIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desde").value("2026-10-01"))
                .andExpect(jsonPath("$.hasta").value("2026-10-03"))
                .andExpect(jsonPath("$.id_usuario_cierre").value(7))
                .andExpect(jsonPath("$.medio_pago").value("EFECTIVO"))
                .andExpect(jsonPath("$.filas[0].id_sesion").value(12))
                .andExpect(jsonPath("$.filas[0].estado").value("CERRADA"))
                .andExpect(jsonPath("$.filas[0].pendiente").value(false))
                .andExpect(jsonPath("$.filas[0].valoresPorMedio.EFECTIVO.faltante").value(5.00))
                .andExpect(jsonPath("$.totales.cantidadSesiones").value(1));

        verify(service).cierresCaja(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3),
                7L, "TODOS", "EFECTIVO");
    }

    @Test
    @DisplayName("Filtros opcionales ausentes: TODOS y sin usuario")
    void sinFiltrosOpcionales() throws Exception {
        when(service.cierresCaja(any(), any(), any(), any(), any())).thenReturn(respuesta());

        mockMvc.perform(get(URL).param("desde", "2026-10-01").param("hasta", "2026-10-03"))
                .andExpect(status().isOk());

        verify(service).cierresCaja(eq(LocalDate.of(2026, 10, 1)),
                eq(LocalDate.of(2026, 10, 3)), isNull(), isNull(), isNull());
    }

    @Test
    @DisplayName("Fecha con formato inválido: 400 y no llama al service")
    void fechaInvalida_devuelve400() throws Exception {
        mockMvc.perform(get(URL).param("desde", "01-10-2026").param("hasta", "2026-10-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El parámetro 'desde' tiene un formato inválido"));
        verify(service, never()).cierresCaja(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Usuario con formato inválido: 400 y no llama al service")
    void usuarioInvalido_devuelve400() throws Exception {
        mockMvc.perform(get(URL).param("desde", "2026-10-01").param("hasta", "2026-10-31")
                        .param("id_usuario_cierre", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje")
                        .value("El parámetro 'id_usuario_cierre' tiene un formato inválido"));
        verify(service, never()).cierresCaja(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Error de negocio del service: 400 con su mensaje")
    void errorDelService_devuelve400() throws Exception {
        when(service.cierresCaja(any(), any(), any(), any(), any()))
                .thenThrow(new DatosInvalidosException("El rango de fechas es obligatorio"));

        mockMvc.perform(get(URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El rango de fechas es obligatorio"));
    }
}

package com.taller.gestion.reportes.controller;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.common.security.ErrorResponseWriter;
import com.taller.gestion.common.security.RestAccessDeniedHandler;
import com.taller.gestion.common.security.RestAuthenticationEntryPoint;
import com.taller.gestion.common.security.SecurityConfig;
import com.taller.gestion.reportes.dto.ComparacionEstacionalFilaDTO;
import com.taller.gestion.reportes.dto.ReporteComparacionEstacionalDTO;
import com.taller.gestion.reportes.dto.TotalesComparacionEstacionalDTO;
import com.taller.gestion.reportes.service.ReporteComparacionEstacionalService;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReporteComparacionEstacionalController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class, ErrorResponseWriter.class})
@WithMockUser(roles = "ADMINISTRADOR")
class ReporteComparacionEstacionalControllerTest {

    private static final String URL = "/api/reportes/evolucion-ventas";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReporteComparacionEstacionalService service;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private ReporteComparacionEstacionalDTO respuesta() {
        ComparacionEstacionalFilaDTO ene = new ComparacionEstacionalFilaDTO(
                1, "ene",
                Map.of("2025", new BigDecimal("100.00"), "2026", new BigDecimal("200.00")),
                Map.of("2026", new BigDecimal("100.00")));
        return new ReporteComparacionEstacionalDTO(
                List.of(2025, 2026), 7L, "Bebidas",
                LocalDate.of(2025, 1, 1), LocalDate.of(2026, 11, 30),
                List.of(ene),
                new TotalesComparacionEstacionalDTO(
                        Map.of("2025", new BigDecimal("100.00"), "2026", new BigDecimal("200.00")),
                        Map.of("2026", new BigDecimal("100.00"))));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Sin sesión: el reporte devuelve 401")
    void sinSesion_devuelve401() throws Exception {
        mockMvc.perform(get(URL).param("anios", "2025,2026"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        verify(service, never()).comparacionEstacional(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    @DisplayName("Rol vendedor: el reporte devuelve 403")
    void conRolVendedor_devuelve403() throws Exception {
        mockMvc.perform(get(URL).param("anios", "2025,2026"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verify(service, never()).comparacionEstacional(any(), any(), any());
    }

    @Test
    @DisplayName("Administrador: 200 con años, filas y totales")
    void conRolAdministrador_devuelveReporte() throws Exception {
        when(service.comparacionEstacional(any(), any(), any())).thenReturn(respuesta());

        mockMvc.perform(get(URL)
                        .param("anios", "2025,2026")
                        .param("id_producto", "7")
                        .param("categoria", "Bebidas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anios[0]").value(2025))
                .andExpect(jsonPath("$.anios[1]").value(2026))
                .andExpect(jsonPath("$.id_producto").value(7))
                .andExpect(jsonPath("$.categoria").value("Bebidas"))
                .andExpect(jsonPath("$.filas[0].etiqueta").value("ene"))
                .andExpect(jsonPath("$.filas[0].totalesPorAnio['2026']").value(200.00))
                .andExpect(jsonPath("$.totales.totalesPorAnio['2025']").value(100.00));

        verify(service).comparacionEstacional("2025,2026", 7L, "Bebidas");
    }

    @Test
    @DisplayName("Filtros opcionales ausentes: sin producto ni categoría")
    void sinFiltrosOpcionales() throws Exception {
        when(service.comparacionEstacional(any(), any(), any())).thenReturn(respuesta());

        mockMvc.perform(get(URL))
                .andExpect(status().isOk());

        verify(service).comparacionEstacional(null, null, null);
    }

    @Test
    @DisplayName("Producto con formato inválido: 400 y no llama al service")
    void productoInvalido_devuelve400() throws Exception {
        mockMvc.perform(get(URL).param("anios", "2025,2026").param("id_producto", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje")
                        .value("El parámetro 'id_producto' tiene un formato inválido"));
        verify(service, never()).comparacionEstacional(any(), any(), any());
    }

    @Test
    @DisplayName("Error de negocio del service: 400 con su mensaje")
    void errorDelService_devuelve400() throws Exception {
        when(service.comparacionEstacional(any(), any(), any()))
                .thenThrow(new DatosInvalidosException("Se pueden comparar hasta 5 años"));

        mockMvc.perform(get(URL).param("anios", "2020,2021,2022,2023,2024,2025"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Se pueden comparar hasta 5 años"));
    }
}

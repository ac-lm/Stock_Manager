package com.taller.gestion.venta.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taller.gestion.common.security.ErrorResponseWriter;
import com.taller.gestion.common.security.RestAccessDeniedHandler;
import com.taller.gestion.common.security.RestAuthenticationEntryPoint;
import com.taller.gestion.caja.dto.CobroVentaDTO;
import com.taller.gestion.caja.entity.MedioPago;
import com.taller.gestion.common.security.SecurityConfig;
import com.taller.gestion.venta.dto.AnularVentaDTO;
import com.taller.gestion.venta.dto.DetalleVentaCreateDTO;
import com.taller.gestion.venta.dto.VentaCreateDTO;
import com.taller.gestion.venta.dto.VentaResponseDTO;
import com.taller.gestion.venta.entity.EstadoVenta;
import com.taller.gestion.venta.service.VentaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VentaController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class, ErrorResponseWriter.class})
@WithMockUser(roles = "VENDEDOR")
class VentaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VentaService ventaService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private VentaResponseDTO respuesta() {
        return new VentaResponseDTO(1L, 1L, "Consumidor final", 5L, null, null,
                new BigDecimal("250.00"), EstadoVenta.CONFIRMADA, null, List.of(),
                List.of(new CobroVentaDTO(MedioPago.EFECTIVO, new BigDecimal("250.00"))));
    }

    private String bodyValido() throws Exception {
        return bodyConPagos(List.of(new CobroVentaDTO(
                MedioPago.EFECTIVO, new BigDecimal("250.00"))));
    }

    private String bodyConPagos(List<CobroVentaDTO> pagos) throws Exception {
        return objectMapper.writeValueAsString(new VentaCreateDTO(null, null,
                List.of(new DetalleVentaCreateDTO(10L, new BigDecimal("2.500"))), pagos));
    }

    @Test
    @DisplayName("POST /api/ventas: alta válida responde 201")
    void crear_valida() throws Exception {
        when(ventaService.crearVenta(any())).thenReturn(respuesta());

        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyValido()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id_venta").value(1))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.total").value(250.00))
                .andExpect(jsonPath("$.pagos[0].medioPago").value("EFECTIVO"))
                .andExpect(jsonPath("$.pagos[0].monto").value(250.00));
    }

    @Test
    @DisplayName("POST /api/ventas sin productos: 400 por validación")
    void crear_sinProductos_devuelve400() throws Exception {
        String body = objectMapper.writeValueAsString(new VentaCreateDTO(null, null, List.of(),
                List.of(new CobroVentaDTO(MedioPago.EFECTIVO, new BigDecimal("250.00")))));

        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/ventas sin pagos: 400 por validación")
    void crear_sinPagos_devuelve400() throws Exception {
        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyConPagos(List.of())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/ventas con pago de monto cero: 400 por validación")
    void crear_pagoCero_devuelve400() throws Exception {
        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyConPagos(List.of(new CobroVentaDTO(
                                MedioPago.TRANSFERENCIA, BigDecimal.ZERO)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/ventas: lista paginada")
    void listar() throws Exception {
        when(ventaService.listarVentas(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(respuesta())));

        mockMvc.perform(get("/api/ventas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id_venta").value(1));
    }

    @Test
    @DisplayName("GET /api/ventas/{id}: devuelve la venta")
    void obtener() throws Exception {
        when(ventaService.obtenerVenta(1L)).thenReturn(respuesta());

        mockMvc.perform(get("/api/ventas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id_cliente").value(1));
    }

    @Test
    @DisplayName("PUT /api/ventas/{id}/anular: anula la venta con motivo")
    void anular() throws Exception {
        VentaResponseDTO anulada = new VentaResponseDTO(1L, 1L, "Consumidor final", 5L,
                null, null, new BigDecimal("250.00"), EstadoVenta.ANULADA, null, List.of(),
                List.of(new CobroVentaDTO(MedioPago.EFECTIVO, new BigDecimal("250.00"))));
        when(ventaService.anularVenta(eq(1L), any())).thenReturn(anulada);

        mockMvc.perform(put("/api/ventas/1/anular")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AnularVentaDTO("Motivo"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ANULADA"));
    }

    @Test
    @DisplayName("PUT /api/ventas/{id}/anular sin motivo: 400 por validación")
    void anular_sinMotivo_devuelve400() throws Exception {
        mockMvc.perform(put("/api/ventas/1/anular")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AnularVentaDTO("  "))))
                .andExpect(status().isBadRequest());
    }
}

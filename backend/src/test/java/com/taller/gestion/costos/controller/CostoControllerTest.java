package com.taller.gestion.costos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taller.gestion.common.exception.OperacionNoPermitidaException;
import com.taller.gestion.common.exception.RecursoNoEncontradoException;
import com.taller.gestion.common.security.ErrorResponseWriter;
import com.taller.gestion.common.security.RestAccessDeniedHandler;
import com.taller.gestion.common.security.RestAuthenticationEntryPoint;
import com.taller.gestion.common.security.SecurityConfig;
import com.taller.gestion.costos.dto.CostoAnularDTO;
import com.taller.gestion.costos.dto.CostoCreateDTO;
import com.taller.gestion.costos.dto.CostoResponseDTO;
import com.taller.gestion.costos.dto.CostoUpdateDTO;
import com.taller.gestion.costos.service.CostoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CostoController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class, ErrorResponseWriter.class})
@WithMockUser(roles = "ADMINISTRADOR")
class CostoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CostoService costoService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private CostoResponseDTO respuesta() {
        return new CostoResponseDTO(10L, 1L, "Alquiler", "Alquiler local",
                new BigDecimal("150.00"), LocalDate.of(2026, 11, 19), 11, 2026,
                7L, null, false, false, null, null, null, "Pago mensual");
    }

    private CostoCreateDTO altaValida() {
        return new CostoCreateDTO(1L, "Alquiler local", new BigDecimal("150.00"),
                LocalDate.of(2026, 11, 19), 11, 2026, null);
    }

    private CostoUpdateDTO edicionValida() {
        return new CostoUpdateDTO(1L, "Alquiler local", new BigDecimal("175.00"),
                LocalDate.of(2026, 11, 20), 11, 2026, null);
    }

    // ============================ SEGURIDAD ============================

    @Test
    @WithAnonymousUser
    @DisplayName("Sin sesión: GET /api/costos devuelve 401")
    void listarSinSesion_devuelve401() throws Exception {
        mockMvc.perform(get("/api/costos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        verify(costoService, never()).buscar(any(), any(), any(), anyBoolean(), any(Pageable.class));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Sin sesión: POST /api/costos devuelve 401")
    void crearSinSesion_devuelve401() throws Exception {
        mockMvc.perform(post("/api/costos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(altaValida())))
                .andExpect(status().isUnauthorized());
        verify(costoService, never()).crear(any());
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    @DisplayName("Rol vendedor: GET /api/costos devuelve 403")
    void listarConRolVendedor_devuelve403() throws Exception {
        mockMvc.perform(get("/api/costos"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verify(costoService, never()).buscar(any(), any(), any(), anyBoolean(), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    @DisplayName("Rol vendedor: POST /api/costos devuelve 403")
    void crearConRolVendedor_devuelve403() throws Exception {
        mockMvc.perform(post("/api/costos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(altaValida())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verify(costoService, never()).crear(any());
    }

    // ============================ CONSULTAS ============================

    @Test
    @DisplayName("GET /api/costos: 200 con la página del service")
    void listar_devuelvePagina() throws Exception {
        Pageable pagina = PageRequest.of(0, 20);
        when(costoService.buscar(any(), any(), any(), anyBoolean(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(respuesta()), pagina, 1));

        mockMvc.perform(get("/api/costos").param("mes", "11").param("anio", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id_costo").value(10))
                .andExpect(jsonPath("$.content[0].nombreTipoCosto").value("Alquiler"))
                .andExpect(jsonPath("$.content[0].id_usuario").value(7));
    }

    @Test
    @DisplayName("GET /api/costos/total: 200 con el total")
    void total_devuelveMonto() throws Exception {
        when(costoService.total(any(), any(), any())).thenReturn(new BigDecimal("300.00"));

        mockMvc.perform(get("/api/costos/total").param("mes", "11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(300.00));
    }

    @Test
    @DisplayName("GET /api/costos/{id}: 200 con el costo")
    void obtener_devuelveCosto() throws Exception {
        when(costoService.findById(10L)).thenReturn(respuesta());

        mockMvc.perform(get("/api/costos/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descripcion").value("Alquiler local"))
                .andExpect(jsonPath("$.monto").value(150.00));
    }

    @Test
    @DisplayName("GET /api/costos/{id} inexistente: 404")
    void obtener_inexistente_devuelve404() throws Exception {
        when(costoService.findById(10L))
                .thenThrow(new RecursoNoEncontradoException("Costo no encontrado"));

        mockMvc.perform(get("/api/costos/10"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Costo no encontrado"));
    }

    // ============================ ESCRITURA ============================

    @Test
    @DisplayName("POST /api/costos válido: 201")
    void crear_valido_devuelve201() throws Exception {
        when(costoService.crear(any())).thenReturn(respuesta());

        mockMvc.perform(post("/api/costos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(altaValida())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id_costo").value(10));
    }

    @Test
    @DisplayName("POST /api/costos con monto inválido: 400 y no llama al service")
    void crear_invalido_devuelve400() throws Exception {
        CostoCreateDTO invalido = new CostoCreateDTO(1L, "Alquiler local", BigDecimal.ZERO,
                LocalDate.of(2026, 11, 19), 11, 2026, null);

        mockMvc.perform(post("/api/costos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles[0]").exists());
        verify(costoService, never()).crear(any());
    }

    @Test
    @DisplayName("PUT /api/costos/{id} válido: 200")
    void actualizar_valido_devuelve200() throws Exception {
        when(costoService.actualizar(any(), any())).thenReturn(respuesta());

        mockMvc.perform(put("/api/costos/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(edicionValida())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id_costo").value(10));
    }

    @Test
    @DisplayName("PUT /api/costos/{id} con descripción vacía: 400")
    void actualizar_invalido_devuelve400() throws Exception {
        CostoUpdateDTO invalido = new CostoUpdateDTO(1L, "", new BigDecimal("175.00"),
                LocalDate.of(2026, 11, 20), 11, 2026, null);

        mockMvc.perform(put("/api/costos/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalido)))
                .andExpect(status().isBadRequest());
        verify(costoService, never()).actualizar(any(), any());
    }

    @Test
    @DisplayName("PATCH /api/costos/{id}/anulacion válido: 200")
    void anular_valido_devuelve200() throws Exception {
        when(costoService.anular(any(), any())).thenReturn(respuesta());

        mockMvc.perform(patch("/api/costos/10/anulacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CostoAnularDTO("Carga duplicada"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /api/costos/{id}/anulacion sin motivo: 400")
    void anular_sinMotivo_devuelve400() throws Exception {
        mockMvc.perform(patch("/api/costos/10/anulacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CostoAnularDTO("  "))))
                .andExpect(status().isBadRequest());
        verify(costoService, never()).anular(any(), any());
    }

    @Test
    @DisplayName("PATCH /api/costos/{id}/anulacion en conflicto: 409")
    void anular_conflicto_devuelve409() throws Exception {
        when(costoService.anular(any(), any()))
                .thenThrow(new OperacionNoPermitidaException("El costo ya está anulado"));

        mockMvc.perform(patch("/api/costos/10/anulacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CostoAnularDTO("Motivo"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El costo ya está anulado"));
    }
}

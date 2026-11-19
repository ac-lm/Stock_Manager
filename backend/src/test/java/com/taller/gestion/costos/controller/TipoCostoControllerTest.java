package com.taller.gestion.costos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taller.gestion.common.exception.OperacionNoPermitidaException;
import com.taller.gestion.common.exception.RecursoDuplicadoException;
import com.taller.gestion.common.exception.RecursoNoEncontradoException;
import com.taller.gestion.common.security.ErrorResponseWriter;
import com.taller.gestion.common.security.RestAccessDeniedHandler;
import com.taller.gestion.common.security.RestAuthenticationEntryPoint;
import com.taller.gestion.common.security.SecurityConfig;
import com.taller.gestion.costos.dto.EstadoDTO;
import com.taller.gestion.costos.dto.TipoCostoCreateDTO;
import com.taller.gestion.costos.dto.TipoCostoResponseDTO;
import com.taller.gestion.costos.dto.TipoCostoUpdateDTO;
import com.taller.gestion.costos.service.TipoCostoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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

@WebMvcTest(TipoCostoController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class, ErrorResponseWriter.class})
@WithMockUser(roles = "ADMINISTRADOR")
class TipoCostoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TipoCostoService tipoCostoService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private TipoCostoResponseDTO respuesta() {
        return new TipoCostoResponseDTO(1L, "Alquiler", true, false);
    }

    // ============================ SEGURIDAD ============================

    @Test
    @WithAnonymousUser
    @DisplayName("Sin sesión: GET /api/tipos-costo devuelve 401")
    void listarSinSesion_devuelve401() throws Exception {
        mockMvc.perform(get("/api/tipos-costo"))
                .andExpect(status().isUnauthorized());
        verify(tipoCostoService, never()).listar(anyBoolean());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Sin sesión: POST /api/tipos-costo devuelve 401")
    void crearSinSesion_devuelve401() throws Exception {
        mockMvc.perform(post("/api/tipos-costo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TipoCostoCreateDTO("Alquiler"))))
                .andExpect(status().isUnauthorized());
        verify(tipoCostoService, never()).crear(any());
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    @DisplayName("Rol vendedor: GET /api/tipos-costo devuelve 403")
    void listarConRolVendedor_devuelve403() throws Exception {
        mockMvc.perform(get("/api/tipos-costo"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verify(tipoCostoService, never()).listar(anyBoolean());
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    @DisplayName("Rol vendedor: POST /api/tipos-costo devuelve 403")
    void crearConRolVendedor_devuelve403() throws Exception {
        mockMvc.perform(post("/api/tipos-costo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TipoCostoCreateDTO("Alquiler"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verify(tipoCostoService, never()).crear(any());
    }

    // ============================ CONSULTAS ============================

    @Test
    @DisplayName("GET /api/tipos-costo: 200 con la lista")
    void listar_devuelveLista() throws Exception {
        when(tipoCostoService.listar(false)).thenReturn(List.of(respuesta()));

        mockMvc.perform(get("/api/tipos-costo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id_tipo_costo").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Alquiler"))
                .andExpect(jsonPath("$[0].reservado").value(false));
        verify(tipoCostoService).listar(false);
    }

    @Test
    @DisplayName("GET /api/tipos-costo?incluirInactivos=true: pasa el filtro al service")
    void listar_conInactivos() throws Exception {
        when(tipoCostoService.listar(true)).thenReturn(List.of());

        mockMvc.perform(get("/api/tipos-costo").param("incluirInactivos", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        verify(tipoCostoService).listar(true);
    }

    // ============================ ESCRITURA ============================

    @Test
    @DisplayName("POST /api/tipos-costo válido: 201")
    void crear_valido_devuelve201() throws Exception {
        when(tipoCostoService.crear(any())).thenReturn(respuesta());

        mockMvc.perform(post("/api/tipos-costo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TipoCostoCreateDTO("Alquiler"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id_tipo_costo").value(1));
    }

    @Test
    @DisplayName("POST /api/tipos-costo con nombre vacío: 400")
    void crear_invalido_devuelve400() throws Exception {
        mockMvc.perform(post("/api/tipos-costo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TipoCostoCreateDTO("  "))))
                .andExpect(status().isBadRequest());
        verify(tipoCostoService, never()).crear(any());
    }

    @Test
    @DisplayName("POST /api/tipos-costo duplicado: 409")
    void crear_duplicado_devuelve409() throws Exception {
        when(tipoCostoService.crear(any()))
                .thenThrow(new RecursoDuplicadoException("Ya existe un tipo con ese nombre"));

        mockMvc.perform(post("/api/tipos-costo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TipoCostoCreateDTO("Alquiler"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe un tipo con ese nombre"));
    }

    @Test
    @DisplayName("PUT /api/tipos-costo/{id} válido: 200")
    void actualizar_valido_devuelve200() throws Exception {
        when(tipoCostoService.actualizar(any(), any())).thenReturn(respuesta());

        mockMvc.perform(put("/api/tipos-costo/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TipoCostoUpdateDTO("Alquiler"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Alquiler"));
    }

    @Test
    @DisplayName("PUT /api/tipos-costo/{id} inexistente: 404")
    void actualizar_inexistente_devuelve404() throws Exception {
        when(tipoCostoService.actualizar(any(), any()))
                .thenThrow(new RecursoNoEncontradoException("Tipo no encontrado"));

        mockMvc.perform(put("/api/tipos-costo/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TipoCostoUpdateDTO("Alquiler"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/tipos-costo/{id}/estado válido: 200")
    void cambiarEstado_valido_devuelve200() throws Exception {
        when(tipoCostoService.cambiarEstado(any(), anyBoolean())).thenReturn(respuesta());

        mockMvc.perform(patch("/api/tipos-costo/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EstadoDTO(true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    @DisplayName("PATCH /api/tipos-costo/{id}/estado sin campo: 400")
    void cambiarEstado_sinCampo_devuelve400() throws Exception {
        mockMvc.perform(patch("/api/tipos-costo/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        verify(tipoCostoService, never()).cambiarEstado(any(), anyBoolean());
    }

    @Test
    @DisplayName("PATCH /api/tipos-costo/{id}/estado del tipo reservado: 409")
    void cambiarEstado_reservado_devuelve409() throws Exception {
        when(tipoCostoService.cambiarEstado(any(), anyBoolean()))
                .thenThrow(new OperacionNoPermitidaException("El tipo es del sistema"));

        mockMvc.perform(patch("/api/tipos-costo/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EstadoDTO(false))))
                .andExpect(status().isConflict());
    }
}

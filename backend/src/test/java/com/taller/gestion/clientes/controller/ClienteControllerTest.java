package com.taller.gestion.clientes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taller.gestion.clientes.dto.ClienteCreateDTO;
import com.taller.gestion.clientes.dto.ClienteResponseDTO;
import com.taller.gestion.clientes.dto.ClienteUpdateDTO;
import com.taller.gestion.clientes.entity.TipoCliente;
import com.taller.gestion.clientes.service.ClienteService;
import com.taller.gestion.common.exception.OperacionNoPermitidaException;
import com.taller.gestion.common.exception.RecursoDuplicadoException;
import com.taller.gestion.common.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClienteController.class)
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClienteService clienteService;

    private ClienteCreateDTO altaValida() {
        return new ClienteCreateDTO("20-12345678-6", "Panadería Sol", "2262-123456",
                "sol@mail.com", "Calle 1 123", TipoCliente.GRAN_CLIENTE, null, 30);
    }

    private ClienteUpdateDTO updateValido() {
        return new ClienteUpdateDTO("20-12345678-6", "Panadería Sol", "2262-123456",
                "sol@mail.com", "Calle 1 123", TipoCliente.GRAN_CLIENTE, null, 30);
    }

    private ClienteResponseDTO respuesta() {
        return new ClienteResponseDTO(1L, "20-12345678-6", "Panadería Sol", "2262-123456",
                "sol@mail.com", "Calle 1 123", TipoCliente.GRAN_CLIENTE, null, true, 30);
    }

    @Test
    @DisplayName("POST alta correcta: 201 y el cliente en el body")
    void alta_correcta_201() throws Exception {
        when(clienteService.save(any())).thenReturn(respuesta());

        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(altaValida())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Panadería Sol"));
    }

    @Test
    @DisplayName("POST con nombre vacío: 400 con el detalle del campo")
    void alta_nombreVacio_400() throws Exception {
        ClienteCreateDTO invalido = new ClienteCreateDTO("20-12345678-6", "   ", null,
                "sol@mail.com", "Calle 1 123", TipoCliente.GRAN_CLIENTE, null, null);

        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").isArray());
    }

    @Test
    @DisplayName("POST con plazo negativo: 400")
    void alta_plazoNegativo_400() throws Exception {
        ClienteCreateDTO invalido = new ClienteCreateDTO("20-12345678-6", "Panadería Sol", null,
                "sol@mail.com", "Calle 1 123", TipoCliente.GRAN_CLIENTE, null, -5);

        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalido)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST con CUIT duplicado: 409")
    void alta_cuitDuplicado_409() throws Exception {
        when(clienteService.save(any()))
                .thenThrow(new RecursoDuplicadoException("Ya existe un cliente con ese CUIT"));

        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(altaValida())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe un cliente con ese CUIT"));
    }

    @Test
    @DisplayName("GET de un cliente inexistente: 404")
    void obtener_inexistente_404() throws Exception {
        when(clienteService.findById(99L))
                .thenThrow(new RecursoNoEncontradoException("Cliente no encontrado con id 99"));

        mockMvc.perform(get("/api/clientes/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT sobre el cliente genérico: 409")
    void update_generico_409() throws Exception {
        when(clienteService.update(anyLong(), any()))
                .thenThrow(new OperacionNoPermitidaException("El cliente genérico no se puede modificar"));

        mockMvc.perform(put("/api/clientes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateValido())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PATCH de estado sin el campo 'activo': 400")
    void cambiarEstado_sinCampo_400() throws Exception {
        mockMvc.perform(patch("/api/clientes/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET del listado: 200 y la página con los clientes")
    void listar_200() throws Exception {
        when(clienteService.buscar(any(), anyBoolean(), any()))
                .thenReturn(new PageImpl<>(List.of(respuesta())));

        mockMvc.perform(get("/api/clientes").param("busqueda", "sol"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombre").value("Panadería Sol"));
    }

    @Test
    @DisplayName("GET de similares: 200 y lista vacía si no hay coincidencias")
    void similares_200() throws Exception {
        when(clienteService.buscarSimilares("perez")).thenReturn(List.of());

        mockMvc.perform(get("/api/clientes/similares").param("nombre", "perez"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
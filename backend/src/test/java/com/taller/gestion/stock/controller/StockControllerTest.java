package com.taller.gestion.stock.controller;

import com.taller.gestion.common.exception.OperacionNoPermitidaException;
import com.taller.gestion.common.exception.RecursoNoEncontradoException;
import com.taller.gestion.common.security.ErrorResponseWriter;
import com.taller.gestion.common.security.RestAccessDeniedHandler;
import com.taller.gestion.common.security.RestAuthenticationEntryPoint;
import com.taller.gestion.common.security.SecurityConfig;
import com.taller.gestion.stock.dto.MovimientoStockResponseDTO;
import com.taller.gestion.stock.dto.StockInsumoResponseDTO;
import com.taller.gestion.stock.dto.StockProductoResponseDTO;
import com.taller.gestion.stock.entity.MotivoMovimientoStock;
import com.taller.gestion.stock.entity.TipoMovimientoStock;
import com.taller.gestion.stock.service.StockService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class, ErrorResponseWriter.class})
@WithMockUser(roles = "VENDEDOR")
class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StockService stockService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private MovimientoStockResponseDTO movimiento() {
        return new MovimientoStockResponseDTO(
                10L, 1L, "Producto 1", null, null,
                TipoMovimientoStock.ENTRADA, MotivoMovimientoStock.AJUSTE,
                new BigDecimal("5.000"), 7L, "Lautaro",
                LocalDateTime.of(2026, 11, 26, 12, 30), "Conteo físico", null);
    }

    private String bodyProducto(String tipo, String motivo, String cantidad, String extra) {
        return """
                {"idProducto":1,"tipo":"%s","motivo":"%s","cantidad":%s%s}
                """.formatted(tipo, motivo, cantidad, extra);
    }

    // ============================== CONSULTAS ==============================

    @Test
    @DisplayName("GET /api/stock/insumos: devuelve saldos con flag de mínimo")
    void listarInsumos() throws Exception {
        when(stockService.listarInsumos()).thenReturn(List.of(new StockInsumoResponseDTO(
                2L, "Aceite", "litros", new BigDecimal("2.000"), 5, true)));

        mockMvc.perform(get("/api/stock/insumos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id_insumo").value(2))
                .andExpect(jsonPath("$[0].stockBajoMinimo").value(true));
    }

    @Test
    @DisplayName("GET /api/stock/productos: devuelve saldos")
    void listarProductos() throws Exception {
        when(stockService.listarProductos()).thenReturn(List.of(new StockProductoResponseDTO(
                1L, "Producto 1", "Prueba", new BigDecimal("10.000"))));

        mockMvc.perform(get("/api/stock/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stockActual").value(10.000));
    }

    @Test
    @DisplayName("GET /api/stock/productos/{id}/movimientos: devuelve el kardex")
    void kardexProducto() throws Exception {
        when(stockService.kardexProducto(1L)).thenReturn(List.of(movimiento()));

        mockMvc.perform(get("/api/stock/productos/1/movimientos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].motivo").value("AJUSTE"))
                .andExpect(jsonPath("$[0].usuario").value("Lautaro"));
    }

    @Test
    @DisplayName("GET kardex de un id inexistente: 404")
    void kardexInexistente() throws Exception {
        when(stockService.kardexInsumo(99L))
                .thenThrow(new RecursoNoEncontradoException("Insumo no encontrado"));

        mockMvc.perform(get("/api/stock/insumos/99/movimientos"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/stock/movimientos: pasa los filtros y pagina")
    void historial() throws Exception {
        when(stockService.historial(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(movimiento()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/stock/movimientos")
                        .param("tipo", "ENTRADA")
                        .param("motivo", "AJUSTE")
                        .param("id_producto", "1")
                        .param("desde", "2026-11-01T00:00:00")
                        .param("hasta", "2026-11-30T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id_movimiento_stock").value(10))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(stockService).historial(eq(TipoMovimientoStock.ENTRADA),
                eq(MotivoMovimientoStock.AJUSTE), eq(1L), eq(null), eq(null),
                any(), any(), any());
    }

    // ============================== MOVIMIENTOS ==============================

    @Test
    @DisplayName("POST /api/stock/productos/movimientos válido: 201")
    void registrarProducto() throws Exception {
        when(stockService.registrarMovimiento(any(), any(), any(), any(), any(), any()))
                .thenReturn(movimiento());

        mockMvc.perform(post("/api/stock/productos/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyProducto("ENTRADA", "AJUSTE", "5.000", "")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("ENTRADA"))
                .andExpect(jsonPath("$.cantidad").value(5.000));

        verify(stockService).registrarMovimiento(
                eq(1L), eq(null), eq(TipoMovimientoStock.ENTRADA),
                eq(MotivoMovimientoStock.AJUSTE), any(), any());
    }

    @Test
    @DisplayName("POST ignora idUsuario del body: el service no lo recibe")
    void registrarIgnoraIdUsuarioDelBody() throws Exception {
        when(stockService.registrarMovimiento(any(), any(), any(), any(), any(), any()))
                .thenReturn(movimiento());
        String body = """
                {"idInsumo":2,"idUsuario":99,"tipo":"ENTRADA","motivo":"COMPRA",
                 "cantidad":5.000}
                """;

        mockMvc.perform(post("/api/stock/insumos/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        verify(stockService).registrarMovimiento(
                eq(null), eq(2L), eq(TipoMovimientoStock.ENTRADA),
                eq(MotivoMovimientoStock.COMPRA), any(), eq(null));
    }

    @Test
    @DisplayName("POST con cantidad cero: 400")
    void registrarCantidadCero() throws Exception {
        mockMvc.perform(post("/api/stock/productos/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyProducto("ENTRADA", "AJUSTE", "0", "")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(stockService);
    }

    @Test
    @DisplayName("POST con motivo INICIAL: 400 (se genera en el alta de catálogo)")
    void registrarMotivoInicial() throws Exception {
        mockMvc.perform(post("/api/stock/productos/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyProducto("ENTRADA", "INICIAL", "1", "")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(stockService);
    }

    @Test
    @DisplayName("POST con producto inexistente: 404")
    void registrarProductoInexistente() throws Exception {
        when(stockService.registrarMovimiento(any(), any(), any(), any(), any(), any()))
                .thenThrow(new RecursoNoEncontradoException("Producto no encontrado"));

        mockMvc.perform(post("/api/stock/productos/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyProducto("ENTRADA", "AJUSTE", "1", "")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST con stock insuficiente: 409")
    void registrarStockInsuficiente() throws Exception {
        when(stockService.registrarMovimiento(any(), any(), any(), any(), any(), any()))
                .thenThrow(new OperacionNoPermitidaException("Stock insuficiente"));

        mockMvc.perform(post("/api/stock/productos/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyProducto("SALIDA", "AJUSTE", "999", "")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Stock insuficiente"));
    }

    @Test
    @DisplayName("Sin sesión, /api/stock/movimientos devuelve 401")
    @WithAnonymousUser
    void sinAutenticacion() throws Exception {
        mockMvc.perform(get("/api/stock/movimientos"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(stockService);
    }
}

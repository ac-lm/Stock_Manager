package com.taller.gestion.stock.controller;

import com.taller.gestion.common.exception.ErrorResponse;
import com.taller.gestion.stock.dto.MovimientoStockCreateDTO;
import com.taller.gestion.stock.dto.MovimientoStockResponseDTO;
import com.taller.gestion.stock.dto.StockInsumoResponseDTO;
import com.taller.gestion.stock.dto.StockProductoResponseDTO;
import com.taller.gestion.stock.entity.MotivoMovimientoStock;
import com.taller.gestion.stock.entity.TipoMovimientoStock;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Stock",
        description = "Saldos, ajustes manuales y kardex de productos e insumos. "
                + "Todos los endpoints requieren sesión iniciada. El usuario se toma de la "
                + "sesión, los movimientos son inmutables y el stock nunca puede ser negativo.")
public interface StockApi {

    @Operation(summary = "Saldos de insumos",
            description = "Incluye el mínimo y el flag stockBajoMinimo.")
    @ApiResponse(responseCode = "200", description = "Lista de insumos con su saldo")
    List<StockInsumoResponseDTO> listarInsumos();

    @Operation(summary = "Saldos de productos")
    @ApiResponse(responseCode = "200", description = "Lista de productos con su saldo")
    List<StockProductoResponseDTO> listarProductos();

    @Operation(summary = "Registrar movimiento de un insumo",
            description = "El usuario se toma de la sesión. Una salida mayor al saldo se "
                    + "rechaza con 409 y no registra nada.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Movimiento registrado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe el insumo",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Stock insuficiente",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<MovimientoStockResponseDTO> registrarMovimientoInsumo(
            MovimientoStockCreateDTO dto);

    @Operation(summary = "Registrar movimiento de un producto",
            description = "El usuario se toma de la sesión. Una salida mayor al saldo se "
                    + "rechaza con 409 y no registra nada.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Movimiento registrado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe el producto",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Stock insuficiente",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<MovimientoStockResponseDTO> registrarMovimientoProducto(
            MovimientoStockCreateDTO dto);

    @Operation(summary = "Kardex de un insumo",
            description = "Movimientos del más reciente al más viejo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Kardex del insumo"),
            @ApiResponse(responseCode = "404", description = "No existe el insumo",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    List<MovimientoStockResponseDTO> kardexInsumo(
            @Parameter(description = "Id del insumo", example = "1") Long id_insumo);

    @Operation(summary = "Kardex de un producto",
            description = "Movimientos del más reciente al más viejo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Kardex del producto"),
            @ApiResponse(responseCode = "404", description = "No existe el producto",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    List<MovimientoStockResponseDTO> kardexProducto(
            @Parameter(description = "Id del producto", example = "1") Long id_producto);

    @Operation(summary = "Historial de movimientos",
            description = "Filtra por tipo, motivo, producto, insumo, usuario y rango de "
                    + "fecha_hora. Orden por defecto: fecha_hora descendente.")
    @ApiResponse(responseCode = "200", description = "Página de movimientos")
    Page<MovimientoStockResponseDTO> historial(
            @Parameter(description = "ENTRADA o SALIDA") TipoMovimientoStock tipo,
            @Parameter(description = "INICIAL, COMPRA, AJUSTE, MERMA o DEVOLUCION")
            MotivoMovimientoStock motivo,
            @Parameter(description = "Id del producto", example = "1") Long id_producto,
            @Parameter(description = "Id del insumo", example = "1") Long id_insumo,
            @Parameter(description = "Id del usuario", example = "1") Long id_usuario,
            @Parameter(description = "Desde (ISO: AAAA-MM-DDTHH:mm:ss)",
                    example = "2026-11-01T00:00:00") LocalDateTime desde,
            @Parameter(description = "Hasta (ISO: AAAA-MM-DDTHH:mm:ss)",
                    example = "2026-11-30T23:59:59") LocalDateTime hasta,
            @ParameterObject Pageable pageable);
}

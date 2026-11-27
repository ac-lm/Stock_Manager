package com.taller.gestion.reportes.controller;

import com.taller.gestion.common.exception.ErrorResponse;
import com.taller.gestion.reportes.dto.ReporteMovimientosStockDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Reportes",
        description = "Consultas de solo lectura sobre los datos de otros módulos. "
                + "Requiere una sesión iniciada con rol ADMINISTRADOR.")
public interface ReporteMovimientosStockApi {

    @Operation(summary = "Entradas, salidas y descartes de stock",
            description = "Movimientos del rango agrupados por artículo. Una salida es por venta "
                    + "cuando lleva id_venta (VEN-10 todavía no las genera), descarte si es MERMA, "
                    + "ajuste si es AJUSTE y 'otras salidas' en cualquier otro caso. El proveedor "
                    + "se filtra por Proveedor_Insumo: solo aplica a insumos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte de movimientos"),
            @ApiResponse(responseCode = "400", description = "Rango o filtro inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "No hay una sesión iniciada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ReporteMovimientosStockDTO movimientosStock(
            @Parameter(description = "Primer día del rango, incluido", example = "2026-11-01")
            String desde,
            @Parameter(description = "Último día del rango, incluido", example = "2026-11-30")
            String hasta,
            @Parameter(description = "TODOS, PRODUCTO o INSUMO", example = "INSUMO")
            String tipo_articulo,
            @Parameter(description = "Id del producto o insumo; requiere tipo_articulo", example = "12")
            String id_articulo,
            @Parameter(description = "TODOS, ENTRADA o SALIDA", example = "SALIDA")
            String tipo_movimiento,
            @Parameter(description = "Id del proveedor de insumos", example = "3")
            String id_proveedor);
}

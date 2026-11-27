package com.taller.gestion.reportes.controller;

import com.taller.gestion.common.exception.ErrorResponse;
import com.taller.gestion.reportes.dto.ReporteVentasPorProductoDTO;
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
public interface ReporteVentasPorProductoApi {

    @Operation(summary = "Ventas por producto y más vendidos",
            description = "Cantidad vendida y suma de Detalle_Venta.subtotal de las ventas "
                    + "confirmadas, agrupado por producto, más un ranking de los N productos "
                    + "con mayor total vendido. Las fechas son inclusivas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte de ventas por producto"),
            @ApiResponse(responseCode = "400", description = "Rango inválido o filtro con formato inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "No hay una sesión iniciada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ReporteVentasPorProductoDTO ventasPorProducto(
            @Parameter(description = "Primer día del rango (incluido)", example = "2026-10-01")
            String desde,
            @Parameter(description = "Último día del rango (incluido)", example = "2026-10-31")
            String hasta,
            @Parameter(description = "Filtrar por un producto", example = "7")
            String id_producto,
            @Parameter(description = "Filtrar por categoría de producto", example = "Bebidas")
            String categoria,
            @Parameter(description = "Tamaño del ranking, por defecto 10", example = "10")
            String top);
}

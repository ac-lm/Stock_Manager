package com.taller.gestion.reportes.controller;

import com.taller.gestion.common.exception.ErrorResponse;
import com.taller.gestion.reportes.dto.ReporteComparacionEstacionalDTO;
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
public interface ReporteComparacionEstacionalApi {

    @Operation(summary = "Evolución de ventas y comparación estacional",
            description = "Total vendido por mes y año de las ventas confirmadas, con una serie "
                    + "por año. Los meses sin ventas valen 0 y los meses futuros del año en "
                    + "curso no se devuelven. Sin filtro de producto se suma Venta.total; con "
                    + "filtro se suma Detalle_Venta.subtotal de las líneas que cumplen.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte de comparación estacional"),
            @ApiResponse(responseCode = "400", description = "Años o filtros con formato inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "No hay una sesión iniciada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ReporteComparacionEstacionalDTO comparacionEstacional(
            @Parameter(description = "Años a comparar, separados por coma. Por defecto el actual "
                    + "y los dos anteriores", example = "2024,2025,2026")
            String anios,
            @Parameter(description = "Filtrar por un producto", example = "7")
            String id_producto,
            @Parameter(description = "Filtrar por categoría de producto", example = "Bebidas")
            String categoria);
}

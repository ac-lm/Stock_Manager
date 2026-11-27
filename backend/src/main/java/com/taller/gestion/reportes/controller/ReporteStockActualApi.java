package com.taller.gestion.reportes.controller;

import com.taller.gestion.common.exception.ErrorResponse;
import com.taller.gestion.reportes.dto.ReporteStockActualDTO;
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
public interface ReporteStockActualApi {

    @Operation(summary = "Stock actual y bajo mínimo",
            description = "Foto de hoy, sin rango de fechas: stock actual de productos e insumos "
                    + "con mínimo, unidad y falta para alcanzarlo. Los productos no tienen mínimo "
                    + "ni unidad (N/A) y nunca se marcan como bajo mínimo. Los insumos no tienen "
                    + "categoría: al filtrar por categoría se muestran solo productos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte de stock actual"),
            @ApiResponse(responseCode = "400", description = "Filtro con formato inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "No hay una sesión iniciada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ReporteStockActualDTO stockActual(
            @Parameter(description = "TODOS, PRODUCTO o INSUMO", example = "INSUMO")
            String tipo,
            @Parameter(description = "Categoría exacta de producto; vacía para todas",
                    example = "Bebidas")
            String categoria,
            @Parameter(description = "Solo artículos con stock menor al mínimo", example = "true")
            String solo_bajo_minimo);
}

package com.taller.gestion.reportes.controller;

import com.taller.gestion.common.exception.ErrorResponse;
import com.taller.gestion.reportes.dto.ReporteIncidenciasDescartesDTO;
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
public interface ReporteIncidenciasDescartesApi {

    @Operation(summary = "Incidencias y descartes por tipo, artículo y estado",
            description = "Incidencias del rango contra los movimientos SALIDA/MERMA del mismo "
                    + "artículo. El cruce es agregado: no hay vínculo 1:1 ni se netean cantidades "
                    + "entre unidades. Un descarte se imprime una sola vez.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte generado"),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Sin sesión",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sin rol ADMINISTRADOR",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ReporteIncidenciasDescartesDTO incidenciasDescartes(
            @Parameter(description = "Primer día incluido (AAAA-MM-DD)", required = true, example = "2026-01-01")
            String desde,
            @Parameter(description = "Último día incluido (AAAA-MM-DD)", required = true, example = "2026-03-31")
            String hasta,
            @Parameter(description = "Id del tipo de incidencia; vacío para todos")
            Long id_tipo_incidencia,
            @Parameter(description = "TODOS, ABIERTA o CERRADA")
            String estado,
            @Parameter(description = "TODOS, PRODUCTO o INSUMO")
            String tipo_articulo,
            @Parameter(description = "Id del producto o insumo; vacío para todos")
            Long id_articulo);
}

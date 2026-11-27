package com.taller.gestion.reportes.controller;

import com.taller.gestion.common.exception.ErrorResponse;
import com.taller.gestion.reportes.dto.ReporteResultadoPeriodoDTO;
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
public interface ReporteResultadoPeriodoApi {

    @Operation(summary = "Resultado del período (ventas menos costos)",
            description = "Totales mensuales de ventas confirmadas por fecha de venta y costos "
                    + "no anulados por periodo_anio/periodo_mes. El resultado es vendido menos "
                    + "costos. Es un resultado simple, no un balance contable.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte de resultado del período"),
            @ApiResponse(responseCode = "400", description = "Rango inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "No hay una sesión iniciada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ReporteResultadoPeriodoDTO resultadoPeriodo(
            @Parameter(description = "Primer mes del rango, inclusivo", example = "2026-10")
            String desde,
            @Parameter(description = "Último mes del rango, inclusivo", example = "2026-12")
            String hasta);
}

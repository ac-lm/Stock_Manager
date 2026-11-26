package com.taller.gestion.reportes.controller;

import com.taller.gestion.common.exception.ErrorResponse;
import com.taller.gestion.reportes.dto.ReporteVentasPorPeriodoDTO;
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
public interface ReporteVentasApi {

    @Operation(summary = "Ventas por período",
            description = "Total vendido, cantidad de ventas y ticket promedio de las ventas "
                    + "confirmadas, agrupado por día, semana o mes. Las fechas son inclusivas. "
                    + "Los períodos sin ventas se devuelven en cero.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte de ventas por período"),
            @ApiResponse(responseCode = "400", description = "Rango inválido o agrupación desconocida",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "No hay una sesión iniciada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ReporteVentasPorPeriodoDTO ventasPorPeriodo(
            @Parameter(description = "Primer día del rango (incluido)", example = "2026-10-01")
            String desde,
            @Parameter(description = "Último día del rango (incluido)", example = "2026-10-31")
            String hasta,
            @Parameter(description = "Agrupación: dia, semana o mes", example = "dia")
            String agrupacion);
}

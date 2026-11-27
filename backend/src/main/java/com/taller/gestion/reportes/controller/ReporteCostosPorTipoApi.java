package com.taller.gestion.reportes.controller;

import com.taller.gestion.common.exception.ErrorResponse;
import com.taller.gestion.reportes.dto.ReporteCostosPorTipoDTO;
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
public interface ReporteCostosPorTipoApi {

    @Operation(summary = "Costos por tipo y su evolución",
            description = "Totales por tipo y mes sobre periodo_anio/periodo_mes, excluyendo "
                    + "anulados. Los meses sin costos se completan en 0. fecha_pago no interviene. "
                    + "La serie de insumos espera a CST-06.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte de costos"),
            @ApiResponse(responseCode = "400", description = "Rango o filtro inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "No hay una sesión iniciada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ReporteCostosPorTipoDTO costosPorTipo(
            @Parameter(description = "Primer mes del rango, inclusivo", example = "2026-10")
            String desde,
            @Parameter(description = "Último mes del rango, inclusivo", example = "2026-12")
            String hasta,
            @Parameter(description = "Id del tipo de costo; ausente para todos", example = "3")
            String id_tipo_costo);
}

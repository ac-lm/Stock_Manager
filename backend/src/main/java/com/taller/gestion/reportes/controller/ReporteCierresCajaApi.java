package com.taller.gestion.reportes.controller;

import com.taller.gestion.common.exception.ErrorResponse;
import com.taller.gestion.reportes.dto.ReporteCierresCajaDTO;
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
public interface ReporteCierresCajaApi {

    @Operation(summary = "Cierres de caja",
            description = "Sesiones cerradas o pendientes de revisión según fecha de apertura, "
                    + "con esperado, declarado, diferencia, ingresos, egresos, faltantes y sobrantes "
                    + "por medio de pago. Las sesiones ABIERTA no se incluyen. El medio de pago "
                    + "acota columnas y totales, sin quitar sesiones. Las fechas son inclusivas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte de cierres de caja"),
            @ApiResponse(responseCode = "400", description = "Rango inválido o filtro con formato inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "No hay una sesión iniciada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ReporteCierresCajaDTO cierresCaja(
            @Parameter(description = "Primer día de apertura (incluido)", example = "2026-10-01")
            String desde,
            @Parameter(description = "Último día de apertura (incluido)", example = "2026-10-31")
            String hasta,
            @Parameter(description = "Filtrar por el usuario que cerró, incluso si está inactivo",
                    example = "7")
            String id_usuario_cierre,
            @Parameter(description = "TODOS, CERRADA o PENDIENTE_REVISION", example = "TODOS")
            String estado,
            @Parameter(description = "TODOS, EFECTIVO o TRANSFERENCIA", example = "TODOS")
            String medio_pago);
}

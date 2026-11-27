package com.taller.gestion.reportes.controller;

import com.taller.gestion.common.exception.ErrorResponse;
import com.taller.gestion.reportes.dto.ReporteVentasPorVendedorDTO;
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
public interface ReporteVentasPorVendedorApi {

    @Operation(summary = "Ventas por vendedor",
            description = "Cantidad de ventas, total vendido y ticket promedio de las ventas "
                    + "confirmadas, agrupado por el usuario que registró cada venta (Venta.id_vendedor). "
                    + "Incluye administradores e usuarios inactivos si registraron ventas. Las fechas "
                    + "son inclusivas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte de ventas por vendedor"),
            @ApiResponse(responseCode = "400", description = "Rango inválido o filtro con formato inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "No hay una sesión iniciada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ReporteVentasPorVendedorDTO ventasPorVendedor(
            @Parameter(description = "Primer día del rango (incluido)", example = "2026-10-01")
            String desde,
            @Parameter(description = "Último día del rango (incluido)", example = "2026-10-31")
            String hasta,
            @Parameter(description = "Filtrar por un vendedor, incluso si está inactivo", example = "7")
            String id_vendedor);
}

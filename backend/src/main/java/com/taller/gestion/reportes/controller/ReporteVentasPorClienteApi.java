package com.taller.gestion.reportes.controller;

import com.taller.gestion.common.exception.ErrorResponse;
import com.taller.gestion.reportes.dto.ReporteVentasPorClienteDTO;
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
public interface ReporteVentasPorClienteApi {

    @Operation(summary = "Ventas por cliente y por tipo de cliente",
            description = "Cantidad de compras y suma de Venta.total de las ventas confirmadas, "
                    + "agrupado por cliente, más el total por tipo. El cliente genérico de "
                    + "mostrador aparece como su propia fila y se suma a 'Consumidor final' en el "
                    + "resumen y en el filtro. Las fechas son inclusivas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte de ventas por cliente"),
            @ApiResponse(responseCode = "400", description = "Rango inválido o filtro con formato inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "No hay una sesión iniciada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ReporteVentasPorClienteDTO ventasPorCliente(
            @Parameter(description = "Primer día del rango (incluido)", example = "2026-10-01")
            String desde,
            @Parameter(description = "Último día del rango (incluido)", example = "2026-10-31")
            String hasta,
            @Parameter(description = "Filtrar por un cliente, incluso si está inactivo", example = "7")
            String id_cliente,
            @Parameter(description = "Filtrar por tipo: GRAN_CLIENTE o CONSUMIDOR_FINAL "
                    + "(incluye al genérico de mostrador)", example = "CONSUMIDOR_FINAL")
            String tipo_cliente);
}

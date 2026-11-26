package com.taller.gestion.reportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Totales del rango completo del reporte de ventas por período")
public record TotalesVentasPorPeriodoDTO(
        @Schema(description = "Total vendido en todo el rango", example = "1750.00")
        BigDecimal totalVendido,

        @Schema(description = "Cantidad de ventas confirmadas en todo el rango", example = "4")
        long cantidadVentas,

        @Schema(description = "Ticket promedio del rango", example = "437.50")
        BigDecimal ticketPromedio
) {
}

package com.taller.gestion.reportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Una fila del reporte de ventas por período")
public record VentasPorPeriodoFilaDTO(
        @Schema(description = "Etiqueta del período: día o lunes (AAAA-MM-DD), mes (AAAA-MM)",
                example = "2026-10")
        String periodo,

        @Schema(description = "Total vendido en el período", example = "1500.00")
        BigDecimal totalVendido,

        @Schema(description = "Cantidad de ventas confirmadas en el período", example = "3")
        long cantidadVentas,

        @Schema(description = "Ticket promedio del período", example = "500.00")
        BigDecimal ticketPromedio
) {
}

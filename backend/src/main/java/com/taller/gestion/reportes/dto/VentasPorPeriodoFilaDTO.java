package com.taller.gestion.reportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Una fila del reporte de ventas por período")
public record VentasPorPeriodoFilaDTO(
        @Schema(description = "Identificador del período: día o lunes (AAAA-MM-DD), mes (AAAA-MM)",
                example = "2026-10")
        String periodo,

        @Schema(description = "Etiqueta del período lista para mostrar: día (AAAA-MM-DD), "
                + "semana (Semana del AAAA-MM-DD) o mes (mmm AAAA)",
                example = "oct 2026")
        String etiqueta,

        @Schema(description = "Total vendido en el período", example = "1500.00")
        BigDecimal totalVendido,

        @Schema(description = "Cantidad de ventas confirmadas en el período", example = "3")
        long cantidadVentas,

        @Schema(description = "Ticket promedio del período", example = "500.00")
        BigDecimal ticketPromedio
) {
}

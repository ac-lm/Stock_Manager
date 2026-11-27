package com.taller.gestion.reportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/** Una fila por mes: ventas confirmadas menos costos no anulados del período. */
@Schema(description = "Total vendido, total de costos y resultado de un mes")
public record ResultadoPeriodoFilaDTO(
        @Schema(description = "Mes en formato YYYY-MM", example = "2026-10")
        String periodo,

        @Schema(description = "Etiqueta del mes lista para mostrar", example = "oct 2026")
        String etiqueta,

        @Schema(description = "Total vendido en la fecha de venta, solo confirmadas",
                example = "750.00")
        BigDecimal totalVendido,

        @Schema(description = "Total de costos del período, anulados excluidos", example = "200.00")
        BigDecimal totalCostos,

        @Schema(description = "Vendido menos costos; puede ser negativo", example = "550.00")
        BigDecimal resultado) {
}

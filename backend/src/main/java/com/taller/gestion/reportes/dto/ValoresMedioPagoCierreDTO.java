package com.taller.gestion.reportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/** Montos de un medio de pago en una sesión o en el total del período. */
@Schema(description = "Esperado, declarado, diferencia, ingresos, egresos, faltantes y sobrantes "
        + "de un medio de pago")
public record ValoresMedioPagoCierreDTO(
        @Schema(description = "Total esperado guardado en el cierre", example = "1500.00")
        BigDecimal esperado,

        @Schema(description = "Total declarado por quien cerró", example = "1490.00")
        BigDecimal declarado,

        @Schema(description = "declarado menos esperado; negativo es faltante", example = "-10.00")
        BigDecimal diferencia,

        @Schema(description = "Suma de ingresos de Movimiento_Caja", example = "1500.00")
        BigDecimal ingresos,

        @Schema(description = "Suma de egresos de Movimiento_Caja, incluidas las anulaciones",
                example = "50.00")
        BigDecimal egresos,

        @Schema(description = "Diferencia negativa en valor absoluto", example = "10.00")
        BigDecimal faltante,

        @Schema(description = "Diferencia positiva", example = "0.00")
        BigDecimal sobrante
) {
}

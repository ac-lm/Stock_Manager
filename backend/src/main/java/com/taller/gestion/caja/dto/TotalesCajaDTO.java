package com.taller.gestion.caja.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Totales de la sesión por medio de pago. En una sesión abierta, "
        + "los declarados y las diferencias son nulos.")
public record TotalesCajaDTO(

        @Schema(description = "Esperado en efectivo: monto inicial + ingresos − egresos")
        BigDecimal esperadoEfectivo,
        @Schema(description = "Esperado por transferencia: ingresos − egresos")
        BigDecimal esperadoTransferencia,
        @Schema(description = "Declarado en efectivo al cerrar", nullable = true)
        BigDecimal declaradoEfectivo,
        @Schema(description = "Neto declarado por transferencia al cerrar; puede ser negativo",
                nullable = true)
        BigDecimal declaradoTransferencia,
        @Schema(description = "Diferencia de efectivo: declarado − esperado", nullable = true)
        BigDecimal diferenciaEfectivo,
        @Schema(description = "Diferencia de transferencia: declarado − esperado", nullable = true)
        BigDecimal diferenciaTransferencia,
        @Schema(description = "Suma de los esperados por medio") BigDecimal totalEsperado,
        @Schema(description = "Suma de los declarados por medio", nullable = true)
        BigDecimal totalDeclarado,
        @Schema(description = "Diferencia global: declarado − esperado", nullable = true)
        BigDecimal diferencia
) {
}

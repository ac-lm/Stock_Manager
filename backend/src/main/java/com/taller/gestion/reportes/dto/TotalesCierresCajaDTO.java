package com.taller.gestion.reportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Totales del período por medio de pago")
public record TotalesCierresCajaDTO(
        @Schema(description = "Cantidad de sesiones incluidas", example = "8")
        long cantidadSesiones,

        @Schema(description = "Ingresos, egresos, faltantes y sobrantes por medio de pago")
        Map<String, ValoresMedioPagoCierreDTO> porMedio
) {
}

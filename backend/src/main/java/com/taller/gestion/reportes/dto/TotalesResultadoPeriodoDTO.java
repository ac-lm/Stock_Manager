package com.taller.gestion.reportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/** Totales del rango: el resultado es vendido menos costos, con dos decimales. */
@Schema(description = "Totales generales del reporte de resultado del período")
public record TotalesResultadoPeriodoDTO(
        @Schema(description = "Suma del total vendido de todos los meses", example = "850.00")
        BigDecimal totalVendido,

        @Schema(description = "Suma de los costos de todos los meses", example = "250.00")
        BigDecimal totalCostos,

        @Schema(description = "Total vendido menos total de costos", example = "600.00")
        BigDecimal resultado,

        @Schema(description = "Cantidad de meses del rango", example = "3")
        int meses) {
}

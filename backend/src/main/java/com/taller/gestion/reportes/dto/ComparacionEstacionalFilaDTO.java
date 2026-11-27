package com.taller.gestion.reportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Map;

@Schema(description = "Una fila por mes del reporte de evolución y comparación estacional")
public record ComparacionEstacionalFilaDTO(
        @Schema(description = "Número de mes, de 1 a 12", example = "1")
        int mes,

        @Schema(description = "Etiqueta del mes lista para mostrar", example = "ene")
        String etiqueta,

        @Schema(description = "Total vendido del mes por año, con el año como clave",
                example = "{\"2026\": 1500.00}")
        Map<String, BigDecimal> totalesPorAnio,

        @Schema(description = "Variación porcentual contra el año anterior, con el año como clave. "
                + "No aparece cuando no hay año anterior o ese año vendió 0")
        Map<String, BigDecimal> variacionesPorAnio
) {
}

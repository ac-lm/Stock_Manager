package com.taller.gestion.reportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Map;

@Schema(description = "Totales por año del reporte de evolución y comparación estacional")
public record TotalesComparacionEstacionalDTO(
        @Schema(description = "Total vendido en los meses mostrados, por año",
                example = "{\"2026\": 1500.00}")
        Map<String, BigDecimal> totalesPorAnio,

        @Schema(description = "Variación porcentual de cada año contra el anterior")
        Map<String, BigDecimal> variacionesPorAnio
) {
}

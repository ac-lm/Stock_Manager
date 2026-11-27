package com.taller.gestion.reportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Map;

/** Una fila por mes del rango; los meses sin costos llevan 0 en todos los tipos. */
@Schema(description = "Totales de un mes, desglosados por tipo de costo")
public record CostoPorTipoFilaDTO(
        @Schema(description = "Mes en formato YYYY-MM", example = "2026-10")
        String periodo,

        @Schema(description = "Etiqueta del mes lista para mostrar", example = "oct 2026")
        String etiqueta,

        @Schema(description = "Total del mes por tipo, con el id del tipo como clave",
                example = "{\"3\": 1500.00}")
        Map<String, BigDecimal> valoresPorTipo,

        @Schema(description = "Total del mes, con dos decimales", example = "1500.00")
        BigDecimal total) {
}

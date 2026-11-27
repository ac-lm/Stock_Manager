package com.taller.gestion.reportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/** Totales del reporte; el total por tipo viaja en {@link TipoCostoReporteDTO}. */
@Schema(description = "Totales generales del reporte de costos")
public record TotalesCostosPorTipoDTO(
        @Schema(description = "Suma de todos los tipos y meses del rango", example = "4500.00")
        BigDecimal totalGeneral,

        @Schema(description = "Cantidad de meses del rango", example = "3")
        int meses,

        @Schema(description = "Cantidad de tipos mostrados en la tabla", example = "2")
        int tipos,

        @Schema(description = "Cantidad de costos no anulados que alimentaron el reporte", example = "8")
        long registros) {
}

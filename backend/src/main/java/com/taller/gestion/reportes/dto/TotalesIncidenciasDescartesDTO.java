package com.taller.gestion.reportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Totales del reporte: solo conteos, nunca sumas de cantidades de unidades distintas. */
@Schema(description = "Totales generales de incidencias y descartes")
public record TotalesIncidenciasDescartesDTO(
        @Schema(description = "Incidencias distintas del rango", example = "4")
        long incidencias,

        @Schema(description = "Incidencias abiertas distintas del rango", example = "2")
        long abiertas,

        @Schema(description = "Incidencias cerradas distintas del rango", example = "2")
        long cerradas,

        @Schema(description = "Movimientos SALIDA/MERMA del rango", example = "2")
        long movimientosMerma,

        @Schema(description = "Artículos con incidencias o descartes", example = "2")
        long articulos) {
}

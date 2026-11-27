package com.taller.gestion.reportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Incidencias distintas de un tipo, para el gráfico de barras del reporte. */
@Schema(description = "Cantidad de incidencias de un tipo en el rango")
public record ConteoIncidenciaPorTipoDTO(
        @Schema(description = "Id del tipo de incidencia", example = "3")
        long idTipoIncidencia,

        @Schema(description = "Nombre del tipo", example = "Rotura")
        String tipoIncidencia,

        @Schema(description = "Incidencias distintas del tipo", example = "4")
        long incidencias) {
}

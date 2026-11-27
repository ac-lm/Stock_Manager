package com.taller.gestion.reportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Opción del selector de tipos de incidencia: activos e inactivos con historial en el rango. */
@Schema(description = "Tipo de incidencia disponible como filtro")
public record TipoIncidenciaOpcionDTO(
        @Schema(description = "Id del tipo de incidencia", example = "3")
        long id,

        @Schema(description = "Nombre del tipo", example = "Rotura")
        String nombre,

        @Schema(description = "True si el tipo sigue activo")
        boolean activo) {
}

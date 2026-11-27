package com.taller.gestion.reportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Opción del selector de tipos: activos, inactivos con costos en el rango y el reservado Insumos. */
@Schema(description = "Tipo de costo disponible como filtro")
public record TipoCostoOpcionDTO(
        @Schema(description = "Id del tipo de costo", example = "3")
        long id,

        @Schema(description = "Nombre del tipo", example = "Alquiler")
        String nombre,

        @Schema(description = "True si el tipo sigue activo")
        boolean activo,

        @Schema(description = "True si es un tipo del sistema, como Insumos")
        boolean reservado) {
}

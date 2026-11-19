package com.taller.gestion.costos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tipo de costo devuelto por la API")
public record TipoCostoResponseDTO(
        @Schema(description = "Id del tipo de costo", example = "1") Long id_tipo_costo,
        @Schema(description = "Nombre del tipo", example = "Alquiler") String nombre,
        @Schema(description = "Indica si se puede usar al cargar costos", example = "true") boolean activo,
        @Schema(description = "Indica si es un tipo del sistema, no editable", example = "false")
        boolean reservado
) {}

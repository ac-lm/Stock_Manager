package com.taller.gestion.costos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Datos para activar o desactivar un tipo de costo")
public record EstadoDTO(
        @Schema(description = "true lo activa; false lo desactiva", example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El estado es obligatorio")
        Boolean activo
) {}

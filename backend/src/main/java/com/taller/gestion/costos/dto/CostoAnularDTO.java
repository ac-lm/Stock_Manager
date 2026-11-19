package com.taller.gestion.costos.dto;

import com.taller.gestion.util.TextoUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para anular un costo. El usuario se toma de la sesión actual.")
public record CostoAnularDTO(
        @Schema(description = "Motivo por el que se anula el costo", example = "Carga duplicada",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El motivo de la anulación es obligatorio")
        @Size(max = 255)
        String motivo
) {
    public CostoAnularDTO {
        motivo = TextoUtil.limpiar(motivo);
    }
}

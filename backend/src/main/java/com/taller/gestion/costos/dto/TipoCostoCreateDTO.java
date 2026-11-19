package com.taller.gestion.costos.dto;

import com.taller.gestion.util.TextoUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para registrar un tipo de costo")
public record TipoCostoCreateDTO(
        @Schema(description = "Nombre del tipo de costo", example = "Alquiler",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 50)
        String nombre
) {
    public TipoCostoCreateDTO {
        nombre = TextoUtil.limpiar(nombre);
    }
}

package com.taller.gestion.costos.dto;

import com.taller.gestion.util.TextoUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para modificar un tipo de costo")
public record TipoCostoUpdateDTO(
        @Schema(description = "Nuevo nombre del tipo de costo", example = "Alquiler de local",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 50)
        String nombre
) {
    public TipoCostoUpdateDTO {
        nombre = TextoUtil.limpiar(nombre);
    }
}

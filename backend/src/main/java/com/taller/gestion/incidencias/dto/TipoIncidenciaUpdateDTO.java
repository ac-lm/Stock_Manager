package com.taller.gestion.incidencias.dto;

import com.taller.gestion.util.TextoUtil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TipoIncidenciaUpdateDTO(
        @NotBlank(message = "El nombre del tipo de incidencia es obligatorio")
        @Size(max = 50, message = "El nombre admite hasta 50 caracteres")
        String nombre,
        Boolean activo
) {
    public TipoIncidenciaUpdateDTO {
        nombre = TextoUtil.limpiar(nombre);
    }
}

package com.taller.gestion.incidencias.dto;

import com.taller.gestion.common.util.TextoUtil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TipoIncidenciaCreateDTO(
        @NotBlank(message = "El nombre del tipo de incidencia es obligatorio")
        @Size(max = 50, message = "El nombre admite hasta 50 caracteres")
        String nombre
) {
    public TipoIncidenciaCreateDTO {
        nombre = TextoUtil.limpiar(nombre);
    }
}

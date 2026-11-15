package com.taller.gestion.incidencias.dto;

import com.taller.gestion.util.TextoUtil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IncidenciaResolucionDTO(
        @NotBlank(message = "La resolución adoptada es obligatoria")
        @Size(max = 100, message = "La resolución adoptada admite hasta 100 caracteres")
        String resolucionAdoptada,
        Long id_usuario_resolucion
) {
    public IncidenciaResolucionDTO {
        resolucionAdoptada = TextoUtil.limpiar(resolucionAdoptada);
    }
}

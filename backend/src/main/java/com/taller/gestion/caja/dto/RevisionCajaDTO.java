package com.taller.gestion.caja.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Revisión de una sesión con diferencia. No modifica los montos.")
public record RevisionCajaDTO(

        @Schema(description = "Observación del administrador sobre la diferencia",
                example = "Se verificó el faltante con el comprobante del banco",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "La observación de revisión es obligatoria")
        @Size(max = 150, message = "La observación admite hasta 150 caracteres")
        String observacion
) {
}

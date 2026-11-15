package com.taller.gestion.usuario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para asignar una nueva contraseña a un usuario")
public record UsuarioPasswordDTO(

        @Schema(description = "Nueva contraseña, entre 8 y 72 bytes", example = "NuevaClave123",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password
) {
}

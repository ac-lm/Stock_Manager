package com.taller.gestion.usuario.dto;

import com.taller.gestion.util.TextoUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Credenciales para iniciar sesión")
public record LoginRequest(

        @Schema(description = "Email del usuario", example = "admin@taller.local",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no es válido")
        @Size(max = 255)
        String email,

        @Schema(description = "Contraseña", example = "Admin1234",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {
    public LoginRequest {
        email = TextoUtil.limpiarEmail(email);
    }
}

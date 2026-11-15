package com.taller.gestion.usuario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Datos para activar o desactivar un usuario")
public record UsuarioEstadoDTO(

        @Schema(description = "true reactiva al usuario; false lo da de baja lógica "
                + "(conserva su historial y deja de poder iniciar sesión)",
                example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El estado es obligatorio")
        Boolean activo
) {
}

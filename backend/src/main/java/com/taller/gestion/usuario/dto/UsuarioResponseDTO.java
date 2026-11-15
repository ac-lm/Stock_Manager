package com.taller.gestion.usuario.dto;

import com.taller.gestion.usuario.entity.Rol;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos públicos de un usuario. Nunca incluye la contraseña.")
public record UsuarioResponseDTO(

        @Schema(description = "Id del usuario", example = "1")
        Long id_usuario,

        @Schema(description = "Nombre y apellido", example = "Ana Pérez")
        String nombre,

        @Schema(description = "Email del usuario", example = "ana@taller.com")
        String email,

        @Schema(description = "Rol del usuario", allowableValues = {"ADMINISTRADOR", "VENDEDOR"},
                example = "VENDEDOR")
        Rol rol,

        @Schema(description = "Indica si el usuario puede iniciar sesión", example = "true")
        boolean activo
) {
}

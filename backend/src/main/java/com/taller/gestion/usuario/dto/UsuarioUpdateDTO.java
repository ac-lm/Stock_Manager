package com.taller.gestion.usuario.dto;

import com.taller.gestion.usuario.entity.Rol;
import com.taller.gestion.util.TextoUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para actualizar un usuario. Los campos nulos no se modifican.")
public record UsuarioUpdateDTO(

        @Schema(description = "Nombre y apellido", example = "Ana Pérez", nullable = true)
        @Size(max = 100)
        String nombre,

        @Schema(description = "Email del usuario", example = "ana@taller.com", nullable = true)
        @Email(message = "El email no es válido")
        @Size(max = 255)
        String email,

        @Schema(description = "Rol del usuario", allowableValues = {"ADMINISTRADOR", "VENDEDOR"},
                example = "VENDEDOR", nullable = true)
        Rol rol
) {
    public UsuarioUpdateDTO {
        nombre = TextoUtil.limpiar(nombre);
        email = TextoUtil.limpiarEmail(email);
    }
}

package com.taller.gestion.usuario.dto;

import com.taller.gestion.usuario.entity.Rol;
import com.taller.gestion.util.TextoUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para registrar un usuario")
public record UsuarioCreateDTO(

        @Schema(description = "Nombre y apellido", example = "Ana Pérez")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100)
        String nombre,

        @Schema(description = "Email del usuario. Es el nombre con el que inicia sesión",
                example = "ana@taller.com")
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no es válido")
        @Size(max = 255)
        String email,

        @Schema(description = "Contraseña inicial, entre 8 y 72 bytes",
                example = "ClaveSegura123")
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password,

        @Schema(description = "Rol del usuario", allowableValues = {"ADMINISTRADOR", "VENDEDOR"},
                example = "VENDEDOR", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El rol es obligatorio")
        Rol rol
) {
    public UsuarioCreateDTO {
        nombre = TextoUtil.limpiar(nombre);
        email = TextoUtil.limpiarEmail(email);
    }
}

package com.taller.gestion.clientes.dto;

import com.taller.gestion.clientes.entity.TipoCliente;
import com.taller.gestion.util.TextoUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Datos para registrar un cliente")
public record ClienteCreateDTO(
        @Schema(description = "CUIT, con o sin guiones. Obligatorio para grandes clientes",
                example = "20-12345678-6", nullable = true)
        String cuit,

        @Schema(description = "Nombre o razón social", example = "Panadería Sol")
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 50)
        String nombre,

        @Schema(description = "Teléfono de contacto", example = "2262-123456", nullable = true)
        @Size(max = 30)
        String telefono,

        @Schema(description = "Correo electrónico", example = "contacto@panaderiasol.com")
        @NotBlank(message = "El email es obligatorio") @Email @Size(max = 150)
        String email,

        @Schema(description = "Dirección", example = "Calle 1 123, Lobería")
        @NotBlank(message = "La dirección es obligatoria") @Size(max = 80)
        String direccion,

        @Schema(description = "Tipo de cliente. GENERICO está reservado para el sistema",
                allowableValues = {"GRAN_CLIENTE", "CONSUMIDOR_FINAL"}, example = "GRAN_CLIENTE")
        @NotNull(message = "El tipo de cliente es obligatorio")
        TipoCliente tipoCliente,

        @Schema(description = "Observaciones", nullable = true)
        @Size(max = 250)
        String observaciones,

        @Schema(description = "Plazo de pago en días. Solo para grandes clientes; vacío significa contado",
                example = "30", nullable = true)
        @Min(value = 0, message = "El plazo no puede ser negativo")
        @Max(value = 365, message = "El plazo no puede superar los 365 días")
        Integer plazoPagoDias
) {
    public ClienteCreateDTO {
        nombre = TextoUtil.limpiar(nombre);
        telefono = TextoUtil.limpiar(telefono);
        email = TextoUtil.limpiarEmail(email);
        direccion = TextoUtil.limpiar(direccion);
        observaciones = TextoUtil.limpiar(observaciones);
    }
}

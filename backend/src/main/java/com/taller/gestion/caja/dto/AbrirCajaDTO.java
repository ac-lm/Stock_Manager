package com.taller.gestion.caja.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Apertura de una sesión de caja. El usuario se toma de la sesión actual.")
public record AbrirCajaDTO(

        @Schema(description = "Dinero en efectivo que hay en la caja al abrir",
                example = "1500.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El monto inicial es obligatorio")
        @DecimalMin(value = "0.00", message = "El monto inicial no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El monto inicial admite hasta 2 decimales")
        BigDecimal montoInicial,

        @Schema(description = "Motivo si el monto difiere del sugerido por el sistema",
                example = "Quedó cambio de la feria", nullable = true)
        @Size(max = 150, message = "El motivo admite hasta 150 caracteres")
        String motivoAjusteInicial
) {
}

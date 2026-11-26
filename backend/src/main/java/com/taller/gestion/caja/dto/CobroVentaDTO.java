package com.taller.gestion.caja.dto;

import com.taller.gestion.caja.entity.MedioPago;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Un pago de la venta. La suma de todos los pagos debe igualar el total.")
public record CobroVentaDTO(

        @Schema(description = "Medio de pago del cobro", example = "EFECTIVO",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El medio de pago es obligatorio")
        MedioPago medioPago,

        @Schema(description = "Monto cobrado por este medio", example = "250.00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El monto del pago es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto del pago debe ser mayor a cero")
        @Digits(integer = 10, fraction = 2, message = "El monto del pago admite hasta 2 decimales")
        BigDecimal monto
) {
}

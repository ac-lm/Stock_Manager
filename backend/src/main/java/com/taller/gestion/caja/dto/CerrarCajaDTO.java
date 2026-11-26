package com.taller.gestion.caja.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Cierre de caja: lo que se contó en efectivo y el neto verificado "
        + "por transferencias (cobros menos pagos, puede ser negativo).")
public record CerrarCajaDTO(

        @Schema(description = "Total declarado en efectivo", example = "1800.00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El declarado en efectivo es obligatorio")
        @DecimalMin(value = "0.00", message = "El declarado en efectivo no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El declarado admite hasta 2 decimales")
        BigDecimal declaradoEfectivo,

        @Schema(description = "Neto declarado por transferencia (cobros menos pagos). "
                + "Puede ser negativo si los egresos superan a los ingresos.",
                example = "-500.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El declarado por transferencia es obligatorio")
        @Digits(integer = 10, fraction = 2, message = "El declarado admite hasta 2 decimales")
        BigDecimal declaradoTransferencia,

        @Schema(description = "Obligatoria si alguna diferencia no es cero",
                example = "Faltaron $200 en efectivo", nullable = true)
        @Size(max = 100, message = "La observación admite hasta 100 caracteres")
        String observaciones
) {
}

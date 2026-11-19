package com.taller.gestion.costos.dto;

import com.taller.gestion.util.TextoUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Datos para modificar un costo. El usuario de alta original no se cambia.")
public record CostoUpdateDTO(
        @Schema(description = "Id del tipo de costo", example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El tipo de costo es obligatorio")
        Long id_tipo_costo,

        @Schema(description = "Descripción del gasto", example = "Alquiler del local",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 255)
        String descripcion,

        @Schema(description = "Monto del costo", example = "175.00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor a 0")
        @Digits(integer = 10, fraction = 2, message = "El monto admite hasta 2 decimales")
        BigDecimal monto,

        @Schema(description = "Fecha en que se pagó", example = "2026-11-20",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "La fecha de pago es obligatoria")
        LocalDate fechaPago,

        @Schema(description = "Mes del período contable (1 a 12)", example = "11",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El mes del período es obligatorio")
        @Min(value = 1, message = "El mes debe estar entre 1 y 12")
        @Max(value = 12, message = "El mes debe estar entre 1 y 12")
        Integer periodoMes,

        @Schema(description = "Año del período contable", example = "2026",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El año del período es obligatorio")
        @Min(value = 2000, message = "El año no es válido")
        @Max(value = 2100, message = "El año no es válido")
        Integer periodoAnio,

        @Schema(description = "Observaciones opcionales", example = "Ajuste por factura corregida")
        @Size(max = 255)
        String observaciones
) {
    public CostoUpdateDTO {
        descripcion = TextoUtil.limpiar(descripcion);
        observaciones = TextoUtil.limpiar(observaciones);
    }
}

package com.taller.gestion.costos.dto;

import com.taller.gestion.util.TextoUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Datos para registrar un costo manual. El usuario se toma de la sesión actual.")
public record CostoCreateDTO(
        @Schema(description = "Id del tipo de costo. No puede ser el tipo reservado de insumos",
                example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El tipo de costo es obligatorio")
        Long id_tipo_costo,

        @Schema(description = "Descripción del gasto", example = "Alquiler del local",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 255)
        String descripcion,

        @Schema(description = "Monto del costo", example = "150.00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor a 0")
        @Digits(integer = 10, fraction = 2, message = "El monto admite hasta 2 decimales")
        BigDecimal monto,

        @Schema(description = "Fecha en que se pagó", example = "2026-11-19",
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

        @Schema(description = "Observaciones opcionales", example = "Pago mensual de noviembre")
        @Size(max = 255)
        String observaciones
) {
    public CostoCreateDTO {
        descripcion = TextoUtil.limpiar(descripcion);
        observaciones = TextoUtil.limpiar(observaciones);
    }
}

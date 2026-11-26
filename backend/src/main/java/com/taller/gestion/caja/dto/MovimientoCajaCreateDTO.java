package com.taller.gestion.caja.dto;

import com.taller.gestion.caja.entity.ConceptoMovimientoCaja;
import com.taller.gestion.caja.entity.MedioPago;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Egreso de caja. En la Fase 1 el concepto solo puede ser RETIRO o GASTO. "
        + "Opcionalmente se vincula con un costo registrado (CST-05).")
public record MovimientoCajaCreateDTO(

        @Schema(description = "Motivo del egreso", example = "RETIRO",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El concepto es obligatorio")
        ConceptoMovimientoCaja concepto,

        @Schema(description = "Medio de pago del egreso", example = "EFECTIVO",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El medio de pago es obligatorio")
        MedioPago medioPago,

        @Schema(description = "Monto que sale de la caja", example = "500.00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
        @Digits(integer = 10, fraction = 2, message = "El monto admite hasta 2 decimales")
        BigDecimal monto,

        @Schema(description = "Detalle libre del egreso", example = "Retiro para depositar",
                nullable = true)
        @Size(max = 150, message = "La observación admite hasta 150 caracteres")
        String observaciones,

        @Schema(description = "Id del costo que se paga con este egreso, si lo hubiere",
                example = "10", nullable = true)
        Long id_costo
) {
}

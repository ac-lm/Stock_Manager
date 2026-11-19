package com.taller.gestion.venta.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Un producto dentro de la venta. El precio no se envía: se toma de la "
        + "lista del tipo de cliente.")
public record DetalleVentaCreateDTO(

        @Schema(description = "Id del producto a vender", example = "1")
        @NotNull(message = "El id del producto es obligatorio")
        Long id_producto_fk,

        @Schema(description = "Cantidad vendida, admite hasta 3 decimales para ventas por peso",
                example = "2.500")
        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor a cero")
        @Digits(integer = 9, fraction = 3, message = "La cantidad admite hasta 3 decimales")
        BigDecimal cantidad
) {
}

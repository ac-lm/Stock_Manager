package com.taller.gestion.venta.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Renglón de una venta ya confirmada")
public record DetalleVentaResponseDTO(

        @Schema(description = "Id del renglón")
        Long id_detalle_venta,

        @Schema(description = "Id de la venta")
        Long id_venta_fk,

        @Schema(description = "Id del producto")
        Long id_producto_fk,

        @Schema(description = "Cantidad vendida")
        BigDecimal cantidad,

        @Schema(description = "Precio unitario que tenía el producto al vender")
        BigDecimal precioHistorico,

        @Schema(description = "cantidad x precioHistorico")
        BigDecimal subtotal
) {
}

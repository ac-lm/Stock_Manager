package com.taller.gestion.stock.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Saldo actual de un producto")
public record StockProductoResponseDTO(

        @Schema(description = "Id del producto") Long id_producto,
        @Schema(description = "Nombre del producto") String nombre,
        @Schema(description = "Categoría del producto") String categoria,
        @Schema(description = "Saldo actual") BigDecimal stockActual
) {
}

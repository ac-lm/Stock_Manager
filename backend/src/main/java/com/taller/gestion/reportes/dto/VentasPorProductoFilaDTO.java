package com.taller.gestion.reportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Una fila del reporte de ventas por producto")
public record VentasPorProductoFilaDTO(
        @Schema(description = "Identificador del producto", example = "7")
        Long id_producto,

        @Schema(description = "Nombre del producto", example = "Café en grano")
        String producto,

        @Schema(description = "Categoría del producto", example = "Bebidas")
        String categoria,

        @Schema(description = "Cantidad vendida en el rango, con hasta tres decimales",
                example = "12.500")
        BigDecimal cantidadVendida,

        @Schema(description = "Suma de los subtotales de las líneas de ventas confirmadas",
                example = "1500.00")
        BigDecimal totalVendido
) {
}

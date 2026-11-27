package com.taller.gestion.reportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Totales del rango del reporte de ventas por producto")
public record TotalesVentasPorProductoDTO(
        @Schema(description = "Cantidad vendida en todo el rango", example = "30.000")
        BigDecimal cantidadVendida,

        @Schema(description = "Total vendido en todo el rango", example = "3500.00")
        BigDecimal totalVendido,

        @Schema(description = "Productos con ventas en el rango", example = "3")
        int productos
) {
}

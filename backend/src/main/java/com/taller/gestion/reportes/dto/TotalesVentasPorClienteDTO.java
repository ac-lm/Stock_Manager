package com.taller.gestion.reportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Totales del rango del reporte de ventas por cliente")
public record TotalesVentasPorClienteDTO(
        @Schema(description = "Cantidad de ventas confirmadas en todo el rango", example = "12")
        long cantidadCompras,

        @Schema(description = "Total vendido en todo el rango", example = "3500.00")
        BigDecimal totalVendido,

        @Schema(description = "Clientes con ventas en el rango", example = "3")
        int clientes
) {
}

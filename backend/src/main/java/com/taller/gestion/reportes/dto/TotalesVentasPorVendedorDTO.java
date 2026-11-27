package com.taller.gestion.reportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Totales del rango del reporte de ventas por vendedor")
public record TotalesVentasPorVendedorDTO(
        @Schema(description = "Cantidad de ventas confirmadas en todo el rango", example = "12")
        long cantidadVentas,

        @Schema(description = "Total vendido en todo el rango", example = "3500.00")
        BigDecimal totalVendido,

        @Schema(description = "Ticket promedio general: total vendido / cantidad de ventas",
                example = "291.67")
        BigDecimal ticketPromedio,

        @Schema(description = "Vendedores con ventas en el rango", example = "3")
        int vendedores
) {
}

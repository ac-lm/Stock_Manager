package com.taller.gestion.reportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Una fila del reporte de ventas por vendedor")
public record VentasPorVendedorFilaDTO(
        @Schema(description = "Identificador del usuario que registró las ventas", example = "7")
        Long id_usuario,

        @Schema(description = "Nombre y rol, con email entre paréntesis si hay otro usuario "
                + "con el mismo nombre", example = "Ana Pérez (Vendedor)")
        String vendedor,

        @Schema(description = "Cantidad de ventas confirmadas del vendedor en el rango", example = "4")
        long cantidadVentas,

        @Schema(description = "Suma de Venta.total de las ventas confirmadas", example = "1500.00")
        BigDecimal totalVendido,

        @Schema(description = "Total vendido dividido por la cantidad de ventas", example = "375.00")
        BigDecimal ticketPromedio
) {
}

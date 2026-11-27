package com.taller.gestion.reportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Una fila del reporte de ventas por cliente")
public record VentasPorClienteFilaDTO(
        @Schema(description = "Identificador del cliente", example = "7")
        Long id_cliente,

        @Schema(description = "Nombre del cliente. El genérico se muestra como "
                + "'Consumidor final (mostrador)'", example = "Panadería Sol")
        String cliente,

        @Schema(description = "Tipo unificado para el reporte: GRAN_CLIENTE o CONSUMIDOR_FINAL. "
                + "El cliente genérico se cuenta como CONSUMIDOR_FINAL",
                example = "GRAN_CLIENTE")
        String tipo_cliente,

        @Schema(description = "Tipo de cliente para mostrar, en español", example = "Gran cliente")
        String tipo,

        @Schema(description = "Cantidad de ventas confirmadas del cliente en el rango", example = "4")
        long cantidadCompras,

        @Schema(description = "Suma de Venta.total de las ventas confirmadas", example = "1500.00")
        BigDecimal totalVendido
) {
}

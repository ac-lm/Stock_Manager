package com.taller.gestion.reportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Total de un tipo de cliente en el resumen del reporte")
public record TotalesPorTipoClienteDTO(
        @Schema(description = "Tipo unificado: GRAN_CLIENTE o CONSUMIDOR_FINAL",
                example = "CONSUMIDOR_FINAL")
        String tipo_cliente,

        @Schema(description = "Tipo de cliente para mostrar, en español",
                example = "Consumidor final")
        String tipo,

        @Schema(description = "Cantidad de compras de ese tipo en el rango", example = "4")
        long cantidadCompras,

        @Schema(description = "Total vendido a ese tipo en el rango", example = "1500.00")
        BigDecimal totalVendido
) {
}

package com.taller.gestion.reportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * No suma stocks: un kg y una unidad no son sumables. Solo cuenta artículos.
 */
@Schema(description = "Cantidades del reporte; nunca suma stocks de unidades distintas")
public record TotalesStockActualDTO(
        @Schema(description = "Artículos incluidos en la tabla", example = "12")
        int articulos,

        @Schema(description = "Artículos bajo mínimo incluidos en la tabla", example = "3")
        int bajoMinimo) {
}

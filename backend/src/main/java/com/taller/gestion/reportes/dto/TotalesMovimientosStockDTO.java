package com.taller.gestion.reportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** No suma cantidades: cada artículo puede tener una unidad distinta. */
@Schema(description = "Cantidades del reporte; nunca suma cantidades de unidades distintas")
public record TotalesMovimientosStockDTO(
        @Schema(description = "Artículos incluidos en la tabla", example = "3")
        int articulos,

        @Schema(description = "Movimientos incluidos en la tabla", example = "12")
        long movimientos) {
}

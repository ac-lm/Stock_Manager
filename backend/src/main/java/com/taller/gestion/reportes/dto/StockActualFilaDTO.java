package com.taller.gestion.reportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/** Una fila por artículo. Mínimo y falta son null cuando el artículo no tiene mínimo cargado. */
@Schema(description = "Artículo con su stock actual, mínimo y falta para alcanzarlo")
public record StockActualFilaDTO(
        @Schema(description = "Id del producto o del insumo", example = "12")
        long id_articulo,

        @Schema(description = "PRODUCTO o INSUMO")
        TipoArticuloStock tipo,

        @Schema(description = "Nombre del artículo", example = "Harina")
        String articulo,

        @Schema(description = "Categoría del producto; los insumos no tienen categoría",
                nullable = true)
        String categoria,

        @Schema(description = "Unidad de medida del insumo; los productos no tienen unidad",
                nullable = true)
        String unidad,

        @Schema(description = "Saldo vigente de la columna stock_actual", example = "2.000")
        BigDecimal stockActual,

        @Schema(description = "Mínimo del insumo; null si el artículo no tiene mínimo (N/A)",
                nullable = true, example = "5")
        Integer stockMinimo,

        @Schema(description = "Máximo(mínimo − stock, 0); null si el mínimo es N/A",
                nullable = true, example = "3.000")
        BigDecimal falta,

        @Schema(description = "True solo si el stock es menor al mínimo de forma estricta")
        boolean bajoMinimo) {
}

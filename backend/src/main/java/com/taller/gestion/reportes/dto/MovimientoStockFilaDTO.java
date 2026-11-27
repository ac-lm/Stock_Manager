package com.taller.gestion.reportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/** Una fila por artículo, con las cantidades agrupadas por clase de movimiento. */
@Schema(description = "Artículo con sus entradas, salidas por venta, descartes y ajustes del período")
public record MovimientoStockFilaDTO(
        @Schema(description = "Id del producto o del insumo", example = "12")
        long id_articulo,

        @Schema(description = "PRODUCTO o INSUMO")
        TipoArticuloStock tipo,

        @Schema(description = "Nombre del artículo", example = "Harina")
        String articulo,

        @Schema(description = "Unidad de medida del insumo; los productos no tienen unidad",
                nullable = true, example = "kg")
        String unidad,

        @Schema(description = "Suma de ENTRADA, incluida INICIAL", example = "9.000")
        BigDecimal entradas,

        @Schema(description = "Suma de SALIDA con id_venta; hoy 0 hasta VEN-10", example = "2.000")
        BigDecimal salidasVenta,

        @Schema(description = "Suma de SALIDA con motivo MERMA", example = "1.000")
        BigDecimal descartes,

        @Schema(description = "Suma de SALIDA con motivo AJUSTE", example = "0.500")
        BigDecimal ajustes,

        @Schema(description = "Suma de SALIDA con otro motivo", example = "0.250")
        BigDecimal otrasSalidas) {
}

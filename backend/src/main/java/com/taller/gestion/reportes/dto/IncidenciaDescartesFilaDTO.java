package com.taller.gestion.reportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/** Una fila por tipo de incidencia, artículo y estado, con sus descartes del mismo artículo. */
@Schema(description = "Incidencias y descartes de un tipo, artículo y estado")
public record IncidenciaDescartesFilaDTO(
        @Schema(description = "Id del tipo; null en la fila sintética sin incidencias", nullable = true)
        Long id_tipo_incidencia,

        @Schema(description = "Nombre del tipo de incidencia", example = "Rotura")
        String tipoIncidencia,

        @Schema(description = "ABIERTA, CERRADA o — cuando no hay incidencia", example = "ABIERTA")
        String estado,

        @Schema(description = "PRODUCTO o INSUMO")
        TipoArticuloStock tipoArticulo,

        @Schema(description = "Id del producto o insumo", example = "12")
        long idArticulo,

        @Schema(description = "Nombre del artículo; — cuando el tipo no tiene incidencias", example = "Harina")
        String articulo,

        @Schema(description = "Unidad del insumo; null para productos", nullable = true, example = "kg")
        String unidad,

        @Schema(description = "Cantidad de incidencias de esta fila", example = "2")
        long incidencias,

        @Schema(description = "Suma de cantidad afectada; no se suma entre unidades", example = "3.500")
        BigDecimal cantidadAfectada,

        @Schema(description = "Incidencias abiertas de esta fila", example = "1")
        long abiertas,

        @Schema(description = "Incidencias cerradas de esta fila", example = "1")
        long cerradas,

        @Schema(description = "Movimientos SALIDA/MERMA del mismo artículo en el rango", example = "1")
        long movimientosMerma,

        @Schema(description = "Cantidad descartada; no se suma entre unidades", example = "2.000")
        BigDecimal cantidadDescartada,

        @Schema(description = "Incidencias y descartes coinciden en el artículo; no se netean")
        boolean posibleTraslape) {
}

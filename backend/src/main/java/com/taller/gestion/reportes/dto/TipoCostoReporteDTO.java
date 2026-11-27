package com.taller.gestion.reportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/** Columna de la tabla y serie del gráfico, con su total en todo el rango. */
@Schema(description = "Tipo de costo incluido en la tabla y en el gráfico")
public record TipoCostoReporteDTO(
        @Schema(description = "Id del tipo de costo", example = "3")
        long id,

        @Schema(description = "Nombre del tipo", example = "Alquiler")
        String nombre,

        @Schema(description = "True si es el tipo reservado Insumos")
        boolean reservado,

        @Schema(description = "Total del tipo en el rango, con dos decimales", example = "1500.00")
        BigDecimal total) {
}

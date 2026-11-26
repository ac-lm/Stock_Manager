package com.taller.gestion.stock.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Saldo actual de un insumo")
public record StockInsumoResponseDTO(

        @Schema(description = "Id del insumo") Long id_insumo,
        @Schema(description = "Nombre del insumo") String nombre,
        @Schema(description = "Unidad de medida") String unidadMedida,
        @Schema(description = "Saldo actual") BigDecimal stockActual,
        @Schema(description = "Stock mínimo") Integer stockMinimo,
        @Schema(description = "true cuando el saldo está por debajo del mínimo")
        boolean stockBajoMinimo
) {
}

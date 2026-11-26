package com.taller.gestion.catalogo.dto;

import java.math.BigDecimal;

/** El stock no se edita desde el catálogo: se modifica con movimientos de `StockService`. */
public record InsumoUpdateDTO(
        String nombre,
        String unidadMedida,
        Integer stockMinimo,
        BigDecimal costo
) {
}

package com.taller.gestion.catalogo.dto;

import java.math.BigDecimal;

/** El stock no se edita desde el catálogo: se modifica con movimientos de `StockService`. */
public record ProductoUpdateDTO(
        String nombre,
        BigDecimal precio,
        String categoria
) {
}

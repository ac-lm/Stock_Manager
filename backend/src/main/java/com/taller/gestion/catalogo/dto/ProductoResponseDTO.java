package com.taller.gestion.catalogo.dto;

import java.math.BigDecimal;

public record ProductoResponseDTO(
        Long id_producto,
        String nombre,
        BigDecimal precio,
        BigDecimal stockActual,
        String categoria
) {
}

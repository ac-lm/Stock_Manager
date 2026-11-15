package com.taller.gestion.catalogo.dto;

import java.math.BigDecimal;

public record InsumoResponseDTO(
        Long id_insumo,
        String nombre,
        BigDecimal stockActual,
        String unidadMedida,
        Integer stockMinimo,
        BigDecimal costo
) {
}

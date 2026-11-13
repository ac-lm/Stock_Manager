package com.taller.gestion.catalogo.dto;

import java.math.BigDecimal;

public record InsumoUpdateDTO(
        String nombre,
        Integer stockActual,
        String unidadMedida,
        Integer stockMinimo,
        BigDecimal costo
) {
}

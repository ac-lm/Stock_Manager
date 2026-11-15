package com.taller.gestion.catalogo.dto;

import java.math.BigDecimal;

public record InsumoCreateDTO(
        String nombre,
        BigDecimal stockActual,
        String unidadMedida,
        Integer stockMinimo,
        BigDecimal costo
) {}

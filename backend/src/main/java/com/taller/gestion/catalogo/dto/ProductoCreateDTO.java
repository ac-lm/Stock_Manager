package com.taller.gestion.catalogo.dto;

import java.math.BigDecimal;

public record ProductoCreateDTO(
        String nombre,
        BigDecimal precio,
        BigDecimal stockActual,
        String categoria
) {}
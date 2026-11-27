package com.taller.gestion.reportes.repository;

import com.taller.gestion.reportes.dto.TipoArticuloStock;

import java.math.BigDecimal;

/** Descartas MERMA agrupados por artículo, sin vínculo 1:1 con ninguna incidencia. */
public record ResumenDescarteIncidencia(
        TipoArticuloStock tipoArticulo,
        long idArticulo,
        String nombre,
        String unidad,
        long movimientos,
        BigDecimal cantidad) {
}

package com.taller.gestion.reportes.repository;

import java.math.BigDecimal;

/** Una incidencia con sus dos posibles artículos; el servicio la desarma en filas por artículo. */
public record DetalleIncidencia(
        long idIncidencia,
        Long idTipoIncidencia,
        String nombreTipo,
        boolean activoTipo,
        String estado,
        Long idProducto,
        String nombreProducto,
        Long idInsumo,
        String nombreInsumo,
        String unidad,
        BigDecimal cantidad) {
}

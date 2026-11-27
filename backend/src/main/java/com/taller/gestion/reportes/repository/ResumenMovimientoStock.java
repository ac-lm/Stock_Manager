package com.taller.gestion.reportes.repository;

import java.math.BigDecimal;

/** Fila agregada por artículo del reporte de movimientos de stock. */
public record ResumenMovimientoStock(
        String tipo,
        long idArticulo,
        String nombre,
        String unidad,
        BigDecimal entradas,
        BigDecimal salidasVenta,
        BigDecimal descartes,
        BigDecimal ajustes,
        BigDecimal otrasSalidas,
        long movimientos) {
}

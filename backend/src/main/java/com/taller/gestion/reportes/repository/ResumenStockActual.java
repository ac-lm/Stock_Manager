package com.taller.gestion.reportes.repository;

import java.math.BigDecimal;

/**
 * Fila cruda de la foto de stock. {@code stockMinimo == null} significa que el artículo no tiene
 * mínimo cargado; los insumos sin mínimo se normalizan a 0 en el servicio, igual que StockService.
 */
public record ResumenStockActual(
        long idArticulo,
        String tipo,
        String nombre,
        String categoria,
        String unidad,
        BigDecimal stockActual,
        Integer stockMinimo) {
}

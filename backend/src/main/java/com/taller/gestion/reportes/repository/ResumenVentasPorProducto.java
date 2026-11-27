package com.taller.gestion.reportes.repository;

import java.math.BigDecimal;

/** Resultado de la consulta agrupada por producto. No es una entidad: solo transporta la fila. */
public record ResumenVentasPorProducto(
        Long idProducto,
        String nombre,
        String categoria,
        BigDecimal cantidadVendida,
        BigDecimal totalVendido
) {
}

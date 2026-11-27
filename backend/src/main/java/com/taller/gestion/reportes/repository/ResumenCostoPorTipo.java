package com.taller.gestion.reportes.repository;

import java.math.BigDecimal;

/** Fila agregada por mes y tipo del reporte de costos. */
public record ResumenCostoPorTipo(
        long idTipoCosto,
        String nombre,
        boolean activo,
        boolean reservado,
        int anio,
        int mes,
        BigDecimal total,
        long registros) {
}

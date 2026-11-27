package com.taller.gestion.reportes.repository;

import java.math.BigDecimal;

/** Total vendido en un par año/mes, ya agrupado por la base de datos. */
public record ResumenVentasPorAnioMes(int anio, int mes, BigDecimal totalVendido) {
}

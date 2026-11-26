package com.taller.gestion.reportes.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Resultado de una consulta agrupada. No es una entidad: solo transporta la fila del SELECT. */
public record ResumenVentasPorPeriodo(
        LocalDate inicio,
        BigDecimal totalVendido,
        long cantidadVentas
) {
}

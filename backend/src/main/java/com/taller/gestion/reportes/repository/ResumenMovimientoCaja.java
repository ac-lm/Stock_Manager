package com.taller.gestion.reportes.repository;

import com.taller.gestion.caja.entity.MedioPago;
import com.taller.gestion.caja.entity.TipoMovimientoCaja;

import java.math.BigDecimal;

/** Suma de movimientos de una sesión, por tipo y medio de pago. */
public record ResumenMovimientoCaja(
        long idSesion,
        MedioPago medioPago,
        TipoMovimientoCaja tipo,
        BigDecimal total
) {
}

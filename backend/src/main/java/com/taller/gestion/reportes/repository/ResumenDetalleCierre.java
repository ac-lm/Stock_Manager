package com.taller.gestion.reportes.repository;

import com.taller.gestion.caja.entity.MedioPago;

import java.math.BigDecimal;

/** Valores vigentes guardados al cerrar, por sesión y medio de pago. */
public record ResumenDetalleCierre(
        long idSesion,
        MedioPago medioPago,
        BigDecimal esperado,
        BigDecimal declarado,
        BigDecimal diferencia
) {
}

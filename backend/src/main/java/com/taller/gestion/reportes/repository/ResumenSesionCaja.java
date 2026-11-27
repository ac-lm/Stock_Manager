package com.taller.gestion.reportes.repository;

import com.taller.gestion.caja.entity.EstadoSesionCaja;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/** Sesión cerrada o pendiente de revisión, con los nombres ya resueltos. */
public record ResumenSesionCaja(
        long idSesion,
        LocalDate fechaApertura,
        LocalTime horaApertura,
        LocalDate fechaCierre,
        LocalTime horaCierre,
        String abrio,
        String cerro,
        BigDecimal montoInicial,
        EstadoSesionCaja estado
) {
}

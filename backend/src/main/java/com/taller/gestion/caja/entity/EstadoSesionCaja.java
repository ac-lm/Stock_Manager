package com.taller.gestion.caja.entity;

/**
 * Estados posibles de una sesión de caja.
 *
 * <p>Una sesión {@code PENDIENTE_REVISION} ya está cerrada para operar: no admite movimientos
 * y no impide abrir la siguiente sesión.</p>
 */
public enum EstadoSesionCaja {
    ABIERTA,
    PENDIENTE_REVISION,
    CERRADA
}

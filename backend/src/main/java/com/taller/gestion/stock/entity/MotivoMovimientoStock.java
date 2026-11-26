package com.taller.gestion.stock.entity;

/**
 * Motivo del movimiento. {@link #INICIAL} se genera automáticamente al dar de alta un
 * producto o insumo con stock; los demás se usan en los ajustes manuales y, más adelante,
 * en las integraciones con otros módulos.
 */
public enum MotivoMovimientoStock {
    INICIAL,
    COMPRA,
    AJUSTE,
    MERMA,
    DEVOLUCION
}

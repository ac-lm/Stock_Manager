package com.taller.gestion.caja.entity;

/**
 * Motivo del movimiento. En la Fase 1 solo se crean movimientos con concepto
 * {@link #RETIRO} o {@link #GASTO}; {@link #VENTA} y {@link #ANULACION} llegan con la
 * integración de ventas.
 */
public enum ConceptoMovimientoCaja {
    VENTA,
    RETIRO,
    GASTO,
    ANULACION
}

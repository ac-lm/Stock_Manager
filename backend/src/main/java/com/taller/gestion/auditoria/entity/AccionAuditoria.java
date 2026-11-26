package com.taller.gestion.auditoria.entity;

/**
 * Operaciones que se dejan constancia en la auditoría. Las tres primeras las genera
 * automáticamente el listener de JPA; el resto son acciones de negocio o de acceso.
 * {@code CORRECCION_AUDITORIA} enmienda una fila manual sin borrarla ni modificarla.
 */
public enum AccionAuditoria {
    ALTA,
    MODIFICACION,
    BAJA,
    LOGIN,
    APERTURA_CAJA,
    CIERRE_CAJA,
    CORRECCION_CAJA,
    ANULACION_VENTA,
    AJUSTE_STOCK,
    CORRECCION_AUDITORIA,
    OTRO
}

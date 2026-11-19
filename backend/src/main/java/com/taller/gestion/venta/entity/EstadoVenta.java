package com.taller.gestion.venta.entity;

/**
 * Una venta se arma y se confirma en el mismo paso, por lo que no existe estado PENDIENTE.
 * Para dejar algo pendiente están Pedido y Presupuesto.
 */
public enum EstadoVenta {
    CONFIRMADA,
    ANULADA
}

package com.taller.gestion.reportes.repository;

import com.taller.gestion.clientes.entity.TipoCliente;

import java.math.BigDecimal;

/** Resultado crudo de la consulta de ventas por cliente, antes de armar el DTO de pantalla. */
public record ResumenVentasPorCliente(
        Long idCliente,
        String nombre,
        TipoCliente tipoCliente,
        long cantidadCompras,
        BigDecimal totalVendido) {
}

package com.taller.gestion.reportes.repository;

import com.taller.gestion.usuario.entity.Rol;

import java.math.BigDecimal;

/** Resultado crudo de la consulta de ventas por vendedor, antes de armar el DTO de pantalla. */
public record ResumenVentasPorVendedor(
        Long idUsuario,
        String nombre,
        String email,
        Rol rol,
        long cantidadVentas,
        BigDecimal totalVendido) {
}

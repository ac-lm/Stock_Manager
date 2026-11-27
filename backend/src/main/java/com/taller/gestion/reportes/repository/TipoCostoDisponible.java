package com.taller.gestion.reportes.repository;

/** Tipo que puede elegirse en el filtro del reporte de costos. */
public record TipoCostoDisponible(
        long id,
        String nombre,
        boolean activo,
        boolean reservado) {
}

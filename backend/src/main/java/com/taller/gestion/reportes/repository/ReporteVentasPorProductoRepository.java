package com.taller.gestion.reportes.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Consultas de solo lectura sobre las líneas de venta. Reportes no tiene tablas propias: se usa
 * SQL nativo estándar (JOIN, GROUP BY y SUM), que funciona igual en PostgreSQL 15 y 17.
 */
@Repository
@RequiredArgsConstructor
public class ReporteVentasPorProductoRepository {

    private final EntityManager entityManager;

    /**
     * @param hastaExclusivo ya calculado en Java como hasta + 1 día, para que el rango sea inclusivo
     *                       sin depender de aritmética de fechas del motor.
     */
    public List<ResumenVentasPorProducto> consultar(
            LocalDate desde, LocalDate hastaExclusivo, Long idProducto, String categoria) {

        StringBuilder sql = new StringBuilder("""
                SELECT p.id_producto, p.nombre, p.categoria,
                       COALESCE(SUM(dv.cantidad), 0) AS cantidad_vendida,
                       COALESCE(SUM(dv.subtotal), 0) AS total_vendido
                FROM Detalle_Venta dv
                JOIN Venta v ON v.id_venta = dv.id_venta_fk
                JOIN Producto p ON p.id_producto = dv.id_producto_fk
                WHERE v.estado = 'CONFIRMADA'
                  AND v.fecha >= :desde
                  AND v.fecha < :hastaExclusivo
                """);
        // Los filtros se agregan por presencia, nunca con texto del usuario dentro del SQL.
        if (idProducto != null) {
            sql.append("  AND dv.id_producto_fk = :idProducto\n");
        }
        if (categoria != null && !categoria.isBlank()) {
            sql.append("  AND p.categoria = :categoria\n");
        }
        sql.append("""
                GROUP BY p.id_producto, p.nombre, p.categoria
                ORDER BY total_vendido DESC, p.nombre ASC
                """);

        Query query = entityManager.createNativeQuery(sql.toString())
                .setParameter("desde", desde)
                .setParameter("hastaExclusivo", hastaExclusivo);
        if (idProducto != null) {
            query.setParameter("idProducto", idProducto);
        }
        if (categoria != null && !categoria.isBlank()) {
            query.setParameter("categoria", categoria);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> filas = query.getResultList();
        return filas.stream()
                .map(fila -> new ResumenVentasPorProducto(
                        ((Number) fila[0]).longValue(),
                        fila[1] == null ? null : fila[1].toString(),
                        fila[2] == null ? null : fila[2].toString(),
                        new BigDecimal(fila[3].toString()),
                        new BigDecimal(fila[4].toString())))
                .toList();
    }
}

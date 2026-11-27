package com.taller.gestion.reportes.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Consultas de solo lectura para la comparación estacional. Reportes no tiene tablas propias:
 * se usa SQL nativo estándar (EXTRACT, JOIN, GROUP BY y SUM), igual en PostgreSQL 15 y 17.
 */
@Repository
@RequiredArgsConstructor
public class ReporteComparacionEstacionalRepository {

    private static final String SIN_FILTRO = """
            SELECT EXTRACT(YEAR FROM v.fecha) AS anio,
                   EXTRACT(MONTH FROM v.fecha) AS mes,
                   COALESCE(SUM(v.total), 0) AS total_vendido
            FROM Venta v
            WHERE v.estado = 'CONFIRMADA'
              AND v.fecha >= :desde
              AND v.fecha < :hastaExclusivo
            GROUP BY EXTRACT(YEAR FROM v.fecha), EXTRACT(MONTH FROM v.fecha)
            ORDER BY anio, mes
            """;

    private static final String CON_FILTRO = """
            SELECT EXTRACT(YEAR FROM v.fecha) AS anio,
                   EXTRACT(MONTH FROM v.fecha) AS mes,
                   COALESCE(SUM(dv.subtotal), 0) AS total_vendido
            FROM Detalle_Venta dv
            JOIN Venta v ON v.id_venta = dv.id_venta_fk
            JOIN Producto p ON p.id_producto = dv.id_producto_fk
            WHERE v.estado = 'CONFIRMADA'
              AND v.fecha >= :desde
              AND v.fecha < :hastaExclusivo
            """;

    private final EntityManager entityManager;

    /**
     * @param hastaExclusivo ya calculado en Java como 1 de enero del año siguiente al último
     *                       comparado, para que el rango sea inclusivo sin aritmética del motor.
     */
    public List<ResumenVentasPorAnioMes> consultar(
            LocalDate desde, LocalDate hastaExclusivo, Long idProducto, String categoria) {

        boolean conFiltro = idProducto != null || (categoria != null && !categoria.isBlank());
        StringBuilder sql = new StringBuilder(conFiltro ? CON_FILTRO : SIN_FILTRO);
        if (conFiltro) {
            if (idProducto != null) {
                sql.append("  AND dv.id_producto_fk = :idProducto\n");
            }
            if (categoria != null && !categoria.isBlank()) {
                sql.append("  AND p.categoria = :categoria\n");
            }
            sql.append("""
                    GROUP BY EXTRACT(YEAR FROM v.fecha), EXTRACT(MONTH FROM v.fecha)
                    ORDER BY anio, mes
                    """);
        }

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
                .map(fila -> new ResumenVentasPorAnioMes(
                        ((Number) fila[0]).intValue(),
                        ((Number) fila[1]).intValue(),
                        new BigDecimal(fila[2].toString())))
                .toList();
    }
}

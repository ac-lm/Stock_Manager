package com.taller.gestion.reportes.repository;

import com.taller.gestion.usuario.entity.Rol;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Consultas de solo lectura sobre ventas y usuarios. Reportes no tiene tablas propias: se usa
 * SQL nativo estándar (JOIN, GROUP BY, COUNT y SUM), que funciona igual en PostgreSQL 15 y 17.
 */
@Repository
@RequiredArgsConstructor
public class ReporteVentasPorVendedorRepository {

    private final EntityManager entityManager;

    /**
     * @param hastaExclusivo ya calculado en Java como hasta + 1 día, para que el rango sea inclusivo
     *                       sin depender de aritmética de fechas del motor.
     */
    public List<ResumenVentasPorVendedor> consultar(
            LocalDate desde, LocalDate hastaExclusivo, Long idVendedor) {

        StringBuilder sql = new StringBuilder("""
                SELECT u.id_usuario, u.nombre, u.email, u.rol,
                       COUNT(v.id_venta) AS cantidad_ventas,
                       COALESCE(SUM(v.total), 0) AS total_vendido
                FROM Venta v
                JOIN Usuario u ON u.id_usuario = v.id_vendedor
                WHERE v.estado = 'CONFIRMADA'
                  AND v.fecha >= :desde
                  AND v.fecha < :hastaExclusivo
                """);
        // Los filtros se agregan por presencia, nunca con texto del usuario dentro del SQL.
        if (idVendedor != null) {
            sql.append("  AND v.id_vendedor = :idVendedor\n");
        }
        sql.append("""
                GROUP BY u.id_usuario, u.nombre, u.email, u.rol
                ORDER BY total_vendido DESC, u.nombre ASC
                """);

        Query query = entityManager.createNativeQuery(sql.toString())
                .setParameter("desde", desde)
                .setParameter("hastaExclusivo", hastaExclusivo);
        if (idVendedor != null) {
            query.setParameter("idVendedor", idVendedor);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> filas = query.getResultList();
        return filas.stream()
                .map(fila -> new ResumenVentasPorVendedor(
                        ((Number) fila[0]).longValue(),
                        fila[1] == null ? null : fila[1].toString(),
                        fila[2] == null ? null : fila[2].toString(),
                        Rol.valueOf(fila[3].toString()),
                        ((Number) fila[4]).longValue(),
                        new BigDecimal(fila[5].toString())))
                .toList();
    }
}

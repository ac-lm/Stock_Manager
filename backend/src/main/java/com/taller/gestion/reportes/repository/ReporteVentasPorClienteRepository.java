package com.taller.gestion.reportes.repository;

import com.taller.gestion.clientes.entity.TipoCliente;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Consultas de solo lectura sobre ventas y clientes. Reportes no tiene tablas propias: se usa
 * SQL nativo estándar (JOIN, GROUP BY, COUNT y SUM), que funciona igual en PostgreSQL 15 y 17.
 */
@Repository
@RequiredArgsConstructor
public class ReporteVentasPorClienteRepository {

    private final EntityManager entityManager;

    /**
     * @param hastaExclusivo ya calculado en Java como hasta + 1 día, para que el rango sea inclusivo
     *                       sin depender de aritmética de fechas del motor.
     * @param tipos          tipos que acepta el filtro, o null/empty si no hay filtro de tipo. Se
     *                       arman marcadores por cada valor: nunca se concatena texto del usuario.
     */
    public List<ResumenVentasPorCliente> consultar(
            LocalDate desde, LocalDate hastaExclusivo, Long idCliente, List<TipoCliente> tipos) {

        StringBuilder sql = new StringBuilder("""
                SELECT c.id_cliente, c.nombre, c.tipo_cliente,
                       COUNT(v.id_venta) AS cantidad_compras,
                       COALESCE(SUM(v.total), 0) AS total_vendido
                FROM Venta v
                JOIN Cliente c ON c.id_cliente = v.id_cliente
                WHERE v.estado = 'CONFIRMADA'
                  AND v.fecha >= :desde
                  AND v.fecha < :hastaExclusivo
                """);
        // Los filtros se agregan por presencia, nunca con texto del usuario dentro del SQL.
        if (idCliente != null) {
            sql.append("  AND v.id_cliente = :idCliente\n");
        }
        if (tipos != null && !tipos.isEmpty()) {
            sql.append("  AND c.tipo_cliente IN (");
            for (int indice = 0; indice < tipos.size(); indice++) {
                if (indice > 0) {
                    sql.append(", ");
                }
                sql.append(":tipo").append(indice);
            }
            sql.append(")\n");
        }
        sql.append("""
                GROUP BY c.id_cliente, c.nombre, c.tipo_cliente
                ORDER BY total_vendido DESC, c.nombre ASC
                """);

        Query query = entityManager.createNativeQuery(sql.toString())
                .setParameter("desde", desde)
                .setParameter("hastaExclusivo", hastaExclusivo);
        if (idCliente != null) {
            query.setParameter("idCliente", idCliente);
        }
        if (tipos != null) {
            for (int indice = 0; indice < tipos.size(); indice++) {
                query.setParameter("tipo" + indice, tipos.get(indice).name());
            }
        }

        @SuppressWarnings("unchecked")
        List<Object[]> filas = query.getResultList();
        return filas.stream()
                .map(fila -> new ResumenVentasPorCliente(
                        ((Number) fila[0]).longValue(),
                        fila[1] == null ? null : fila[1].toString(),
                        TipoCliente.valueOf(fila[2].toString()),
                        ((Number) fila[3]).longValue(),
                        new BigDecimal(fila[4].toString())))
                .toList();
    }
}

package com.taller.gestion.reportes.repository;

import com.taller.gestion.reportes.dto.AgrupacionPeriodo;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Consultas de solo lectura sobre Venta. Reportes no tiene tablas propias: se usa SQL nativo
 * estándar (GROUP BY, SUM, COUNT y date_trunc), que funciona igual en PostgreSQL 15 y 17.
 */
@Repository
@RequiredArgsConstructor
public class ReporteVentasRepository {

    private static final String POR_DIA = "v.fecha";
    private static final String POR_SEMANA = "date_trunc('week', v.fecha)::date";
    private static final String POR_MES = "date_trunc('month', v.fecha)::date";

    private static final String CONSULTA = """
            SELECT %s AS inicio,
                   COALESCE(SUM(v.total), 0) AS total_vendido,
                   COUNT(v.id_venta) AS cantidad_ventas
            FROM venta v
            WHERE v.estado = 'CONFIRMADA'
              AND v.fecha >= :desde
              AND v.fecha < :hastaExclusivo
            GROUP BY %s
            ORDER BY %s
            """;

    private final EntityManager entityManager;

    /**
     * @param hastaExclusivo ya calculado en Java como hasta + 1 día, para que el rango sea inclusivo
     *                       sin depender de aritmética de fechas del motor.
     */
    public List<ResumenVentasPorPeriodo> consultar(
            AgrupacionPeriodo agrupacion, LocalDate desde, LocalDate hastaExclusivo) {

        String expresion = switch (agrupacion) {
            case DIA -> POR_DIA;
            case SEMANA -> POR_SEMANA;
            case MES -> POR_MES;
        };

        @SuppressWarnings("unchecked")
        List<Object[]> filas = entityManager
                .createNativeQuery(String.format(CONSULTA, expresion, expresion, expresion))
                .setParameter("desde", desde)
                .setParameter("hastaExclusivo", hastaExclusivo)
                .getResultList();

        return filas.stream()
                .map(fila -> new ResumenVentasPorPeriodo(
                        aFecha(fila[0]),
                        new BigDecimal(fila[1].toString()),
                        ((Number) fila[2]).longValue()))
                .toList();
    }

    private LocalDate aFecha(Object valor) {
        if (valor instanceof LocalDate fecha) {
            return fecha;
        }
        if (valor instanceof java.sql.Date fecha) {
            return fecha.toLocalDate();
        }
        return LocalDate.parse(valor.toString());
    }
}

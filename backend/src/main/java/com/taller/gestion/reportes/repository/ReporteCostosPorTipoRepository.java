package com.taller.gestion.reportes.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Costos de solo lectura para REP-10. El período es {@code periodo_anio} + {@code periodo_mes}:
 * {@code fecha_pago} no interviene. Los anulados se excluyen siempre, igual que /api/costos/total.
 */
@Repository
@RequiredArgsConstructor
public class ReporteCostosPorTipoRepository {

    private final EntityManager entityManager;

    /** @param desdeClave anio * 100 + mes del primer mes del rango */
    public List<ResumenCostoPorTipo> consultar(int desdeClave, int hastaClave, Long idTipoCosto) {
        String sql = """
                SELECT tc.id_tipo_costo, tc.nombre, tc.activo, tc.reservado,
                       c.periodo_anio, c.periodo_mes, SUM(c.monto), COUNT(*)
                FROM costo c
                JOIN tipo_costo tc ON tc.id_tipo_costo = c.id_tipo_costo
                WHERE c.anulado = false
                  AND (c.periodo_anio * 100 + c.periodo_mes) BETWEEN :desdeClave AND :hastaClave
                """ + (idTipoCosto == null ? "" : "  AND c.id_tipo_costo = :idTipoCosto\n")
                + """
                GROUP BY 1, 2, 3, 4, 5, 6
                ORDER BY 5, 6, 2
                """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("desdeClave", desdeClave);
        query.setParameter("hastaClave", hastaClave);
        if (idTipoCosto != null) {
            query.setParameter("idTipoCosto", idTipoCosto);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> filas = query.getResultList();
        return filas.stream()
                .map(fila -> new ResumenCostoPorTipo(
                        ((Number) fila[0]).longValue(),
                        fila[1].toString(),
                        (Boolean) fila[2],
                        (Boolean) fila[3],
                        ((Number) fila[4]).intValue(),
                        ((Number) fila[5]).intValue(),
                        new BigDecimal(fila[6].toString()),
                        ((Number) fila[7]).longValue()))
                .toList();
    }

    /**
     * Selector: tipos activos, inactivos con costos en el rango y el reservado Insumos.
     * Un tipo inactivo sin costos en el período no aparece para no ocultar historial ni inventar
     * columnas vacías.
     */
    public List<TipoCostoDisponible> tiposDisponibles(int desdeClave, int hastaClave) {
        String sql = """
                SELECT tc.id_tipo_costo, tc.nombre, tc.activo, tc.reservado
                FROM tipo_costo tc
                WHERE tc.activo = true
                   OR tc.reservado = true
                   OR EXISTS (SELECT 1 FROM costo c
                              WHERE c.id_tipo_costo = tc.id_tipo_costo
                                AND c.anulado = false
                                AND (c.periodo_anio * 100 + c.periodo_mes)
                                    BETWEEN :desdeClave AND :hastaClave)
                ORDER BY tc.nombre
                """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("desdeClave", desdeClave);
        query.setParameter("hastaClave", hastaClave);

        @SuppressWarnings("unchecked")
        List<Object[]> filas = query.getResultList();
        return filas.stream()
                .map(fila -> new TipoCostoDisponible(
                        ((Number) fila[0]).longValue(),
                        fila[1].toString(),
                        (Boolean) fila[2],
                        (Boolean) fila[3]))
                .toList();
    }
}

package com.taller.gestion.reportes.repository;

import com.taller.gestion.caja.entity.EstadoSesionCaja;
import com.taller.gestion.caja.entity.MedioPago;
import com.taller.gestion.caja.entity.TipoMovimientoCaja;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Consultas de solo lectura para el reporte de cierres. Reportes no tiene tablas propias: usa SQL
 * nativo estándar (JOIN, GROUP BY y SUM), igual en PostgreSQL 15 y 17.
 *
 * <p>El período se filtra por fecha de apertura, igual que el historial de caja. Siempre se
 * excluyen las sesiones ABIERTA: no tienen cierre ni valores declarados.</p>
 */
@Repository
@RequiredArgsConstructor
public class ReporteCierresCajaRepository {

    private final EntityManager entityManager;

    public List<ResumenSesionCaja> listarSesiones(
            LocalDate desde, LocalDate hastaExclusivo, Long idUsuarioCierre,
            EstadoSesionCaja estado) {

        String sql = """
                SELECT s.id_sesion, s.fecha_apertura, s.hora_apertura, s.fecha_cierre,
                       s.hora_cierre, ua.nombre, uc.nombre, s.monto_inicial, s.estado
                FROM Sesion_Caja s
                JOIN Usuario ua ON ua.id_usuario = s.id_usuario_apertura
                LEFT JOIN Usuario uc ON uc.id_usuario = s.id_usuario_cierre
                """ + filtroComun(idUsuarioCierre, estado) + """
                ORDER BY s.fecha_apertura, s.hora_apertura, s.id_sesion
                """;

        @SuppressWarnings("unchecked")
        List<Object[]> filas = consultar(sql, desde, hastaExclusivo, idUsuarioCierre, estado)
                .getResultList();
        return filas.stream()
                .map(fila -> new ResumenSesionCaja(
                        ((Number) fila[0]).longValue(),
                        aFecha(fila[1]),
                        aHora(fila[2]),
                        fila[3] == null ? null : aFecha(fila[3]),
                        fila[4] == null ? null : aHora(fila[4]),
                        fila[5] == null ? null : fila[5].toString(),
                        fila[6] == null ? null : fila[6].toString(),
                        new BigDecimal(fila[7].toString()),
                        EstadoSesionCaja.valueOf(fila[8].toString())))
                .toList();
    }

    /** Los valores son los vigentes guardados al cerrar; CAJA-08 todavía no existe. */
    public List<ResumenDetalleCierre> listarDetalles(
            LocalDate desde, LocalDate hastaExclusivo, Long idUsuarioCierre,
            EstadoSesionCaja estado) {

        String sql = """
                SELECT cc.id_sesion, cc.medio_pago, cc.total_esperado, cc.total_declarado,
                       cc.diferencia
                FROM Cierre_Caja_Detalle cc
                JOIN Sesion_Caja s ON s.id_sesion = cc.id_sesion
                """ + filtroComun(idUsuarioCierre, estado) + """
                ORDER BY cc.id_sesion, cc.medio_pago
                """;

        @SuppressWarnings("unchecked")
        List<Object[]> filas = consultar(sql, desde, hastaExclusivo, idUsuarioCierre, estado)
                .getResultList();
        return filas.stream()
                .map(fila -> new ResumenDetalleCierre(
                        ((Number) fila[0]).longValue(),
                        MedioPago.valueOf(fila[1].toString()),
                        new BigDecimal(fila[2].toString()),
                        new BigDecimal(fila[3].toString()),
                        new BigDecimal(fila[4].toString())))
                .toList();
    }

    /**
     * Suma los movimientos tal como están. Las anulaciones son EGRESO/ANULACION (ADR 0002): no se
     * filtra por el estado de Venta, porque ese estado vive en otro módulo.
     */
    public List<ResumenMovimientoCaja> listarMovimientos(
            LocalDate desde, LocalDate hastaExclusivo, Long idUsuarioCierre,
            EstadoSesionCaja estado) {

        String sql = """
                SELECT m.id_sesion_caja, m.medio_pago, m.tipo, COALESCE(SUM(m.monto), 0)
                FROM Movimiento_Caja m
                JOIN Sesion_Caja s ON s.id_sesion = m.id_sesion_caja
                """ + filtroComun(idUsuarioCierre, estado) + """
                GROUP BY m.id_sesion_caja, m.medio_pago, m.tipo
                ORDER BY m.id_sesion_caja, m.medio_pago, m.tipo
                """;

        @SuppressWarnings("unchecked")
        List<Object[]> filas = consultar(sql, desde, hastaExclusivo, idUsuarioCierre, estado)
                .getResultList();
        return filas.stream()
                .map(fila -> new ResumenMovimientoCaja(
                        ((Number) fila[0]).longValue(),
                        MedioPago.valueOf(fila[1].toString()),
                        TipoMovimientoCaja.valueOf(fila[2].toString()),
                        new BigDecimal(fila[3].toString())))
                .toList();
    }

    private String filtroComun(Long idUsuarioCierre, EstadoSesionCaja estado) {
        StringBuilder sql = new StringBuilder("""
                WHERE s.fecha_apertura >= :desde
                  AND s.fecha_apertura < :hastaExclusivo
                  AND s.estado IN ('CERRADA', 'PENDIENTE_REVISION')
                """);
        if (estado != null) {
            sql.append("  AND s.estado = :estado\n");
        }
        if (idUsuarioCierre != null) {
            sql.append("  AND s.id_usuario_cierre = :idUsuarioCierre\n");
        }
        return sql.toString();
    }

    private Query consultar(String sql, LocalDate desde, LocalDate hastaExclusivo,
                            Long idUsuarioCierre, EstadoSesionCaja estado) {
        Query query = entityManager.createNativeQuery(sql)
                .setParameter("desde", desde)
                .setParameter("hastaExclusivo", hastaExclusivo);
        if (estado != null) {
            query.setParameter("estado", estado.name());
        }
        if (idUsuarioCierre != null) {
            query.setParameter("idUsuarioCierre", idUsuarioCierre);
        }
        return query;
    }

    private LocalDate aFecha(Object valor) {
        return LocalDate.parse(valor.toString().substring(0, 10));
    }

    private LocalTime aHora(Object valor) {
        String texto = valor.toString();
        return LocalTime.parse(texto.length() > 5 ? texto.substring(0, 8) : texto);
    }
}

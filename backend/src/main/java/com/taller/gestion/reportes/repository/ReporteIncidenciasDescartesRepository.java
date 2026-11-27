package com.taller.gestion.reportes.repository;

import com.taller.gestion.reportes.dto.TipoArticuloStock;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Solo lectura sobre Incidencia y Movimiento_Stock. No agrega FK ni motivos: el cruce es agregado
 * por artículo y período, porque hoy no existe vínculo 1:1 entre una incidencia y un descarte.
 */
@Repository
@RequiredArgsConstructor
public class ReporteIncidenciasDescartesRepository {

    private final EntityManager entityManager;

    /** Trae cada incidencia con sus dos posibles artículos para que el servicio cuente cada uno. */
    public List<DetalleIncidencia> consultarIncidencias(
            LocalDateTime desde, LocalDateTime hastaExclusivo, Long idTipo, String estado,
            TipoArticuloStock tipoArticulo, Long idArticulo) {

        List<String> condiciones = new ArrayList<>();
        condiciones.add("i.fecha >= :desde");
        condiciones.add("i.fecha < :hastaExclusivo");
        if (idTipo != null) {
            condiciones.add("i.id_tipo_incidencia = :idTipo");
        }
        if (estado != null) {
            condiciones.add("i.estado = :estado");
        }
        if (tipoArticulo == TipoArticuloStock.PRODUCTO) {
            condiciones.add("i.id_producto IS NOT NULL");
        } else if (tipoArticulo == TipoArticuloStock.INSUMO) {
            condiciones.add("i.id_insumo IS NOT NULL");
        }
        if (idArticulo != null) {
            condiciones.add(tipoArticulo == TipoArticuloStock.PRODUCTO
                    ? "i.id_producto = :idArticulo" : "i.id_insumo = :idArticulo");
        }

        String sql = """
                SELECT i.id_incidencia, ti.id_tipo_incidencia, ti.nombre, ti.activo, i.estado,
                       i.id_producto, p.nombre, i.id_insumo, ins.nombre, ins.unidad_medida,
                       i.cantidad_afectada
                  FROM Incidencia i
                  JOIN Tipo_Incidencia ti ON ti.id_tipo_incidencia = i.id_tipo_incidencia
                  LEFT JOIN Producto p ON p.id_producto = i.id_producto
                  LEFT JOIN Insumo ins ON ins.id_insumo = i.id_insumo
                 WHERE """ + "\n  " + String.join("\n  AND ", condiciones)
                + "\n ORDER BY ti.nombre, i.fecha, i.id_incidencia\n";

        Query query = entityManager.createNativeQuery(sql);
        parametrosComunes(query, desde, hastaExclusivo, idTipo, estado, idArticulo);

        @SuppressWarnings("unchecked")
        List<Object[]> filas = query.getResultList();
        return filas.stream()
                .map(fila -> new DetalleIncidencia(
                        ((Number) fila[0]).longValue(),
                        ((Number) fila[1]).longValue(),
                        texto(fila[2]),
                        (Boolean) fila[3],
                        texto(fila[4]),
                        id(fila[5]),
                        texto(fila[6]),
                        id(fila[7]),
                        texto(fila[8]),
                        texto(fila[9]),
                        decimal(fila[10])))
                .toList();
    }

    /** Descarte es exactamente SALIDA con motivo MERMA y sin id_venta, como en REP-09. */
    public List<ResumenDescarteIncidencia> consultarDescartes(
            LocalDateTime desde, LocalDateTime hastaExclusivo,
            TipoArticuloStock tipoArticulo, Long idArticulo) {

        List<String> condiciones = new ArrayList<>();
        condiciones.add("m.tipo = 'SALIDA'");
        condiciones.add("m.motivo = 'MERMA'");
        condiciones.add("m.id_venta IS NULL");
        condiciones.add("m.fecha_hora >= :desde");
        condiciones.add("m.fecha_hora < :hastaExclusivo");
        if (tipoArticulo == TipoArticuloStock.PRODUCTO) {
            condiciones.add("m.id_producto IS NOT NULL");
        } else if (tipoArticulo == TipoArticuloStock.INSUMO) {
            condiciones.add("m.id_insumo IS NOT NULL");
        }
        if (idArticulo != null) {
            condiciones.add(tipoArticulo == TipoArticuloStock.PRODUCTO
                    ? "m.id_producto = :idArticulo" : "m.id_insumo = :idArticulo");
        }

        String sql = """
                SELECT CASE WHEN m.id_producto IS NOT NULL THEN 'PRODUCTO' ELSE 'INSUMO' END AS tipo,
                       COALESCE(m.id_producto, m.id_insumo) AS id_articulo,
                       COALESCE(p.nombre, ins.nombre) AS nombre,
                       ins.unidad_medida AS unidad,
                       COUNT(*) AS movimientos,
                       COALESCE(SUM(m.cantidad), 0) AS cantidad
                  FROM Movimiento_Stock m
                  LEFT JOIN Producto p ON p.id_producto = m.id_producto
                  LEFT JOIN Insumo ins ON ins.id_insumo = m.id_insumo
                 WHERE """ + "\n  " + String.join("\n  AND ", condiciones)
                + "\n GROUP BY 1, 2, 3, 4\n ORDER BY nombre, tipo\n";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("desde", desde);
        query.setParameter("hastaExclusivo", hastaExclusivo);
        if (idArticulo != null) {
            query.setParameter("idArticulo", idArticulo);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> filas = query.getResultList();
        return filas.stream()
                .map(fila -> new ResumenDescarteIncidencia(
                        TipoArticuloStock.valueOf(fila[0].toString()),
                        ((Number) fila[1]).longValue(),
                        texto(fila[2]),
                        texto(fila[3]),
                        ((Number) fila[4]).longValue(),
                        decimal(fila[5])))
                .toList();
    }

    /** Activos e inactivos con incidencias en el rango, como hace REP-10 con los tipos de costo. */
    public List<TipoIncidenciaDisponible> tiposDisponibles(
            LocalDateTime desde, LocalDateTime hastaExclusivo) {
        String sql = """
                SELECT ti.id_tipo_incidencia, ti.nombre, ti.activo
                  FROM Tipo_Incidencia ti
                 WHERE ti.activo = true
                    OR EXISTS (SELECT 1 FROM Incidencia i
                                WHERE i.id_tipo_incidencia = ti.id_tipo_incidencia
                                  AND i.fecha >= :desde
                                  AND i.fecha < :hastaExclusivo)
                 ORDER BY ti.nombre
                """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("desde", desde);
        query.setParameter("hastaExclusivo", hastaExclusivo);

        @SuppressWarnings("unchecked")
        List<Object[]> filas = query.getResultList();
        return filas.stream()
                .map(fila -> new TipoIncidenciaDisponible(
                        ((Number) fila[0]).longValue(),
                        texto(fila[1]),
                        (Boolean) fila[2]))
                .toList();
    }

    private void parametrosComunes(Query query, LocalDateTime desde, LocalDateTime hastaExclusivo,
                                   Long idTipo, String estado, Long idArticulo) {
        query.setParameter("desde", desde);
        query.setParameter("hastaExclusivo", hastaExclusivo);
        if (idTipo != null) {
            query.setParameter("idTipo", idTipo);
        }
        if (estado != null) {
            query.setParameter("estado", estado);
        }
        if (idArticulo != null) {
            query.setParameter("idArticulo", idArticulo);
        }
    }

    private Long id(Object valor) {
        return valor == null ? null : ((Number) valor).longValue();
    }

    private String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }

    private BigDecimal decimal(Object valor) {
        if (valor == null) {
            return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        }
        return new BigDecimal(valor.toString());
    }
}

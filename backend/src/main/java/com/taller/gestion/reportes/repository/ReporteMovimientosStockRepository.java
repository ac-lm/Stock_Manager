package com.taller.gestion.reportes.repository;

import com.taller.gestion.reportes.dto.TipoArticuloStock;
import com.taller.gestion.stock.entity.TipoMovimientoStock;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Movimientos de stock de solo lectura. Agrupa por artículo, nunca por unidad mezclando kg con
 * unidades: cada fila corresponde a un solo producto o insumo y a una sola unidad.
 */
@Repository
@RequiredArgsConstructor
public class ReporteMovimientosStockRepository {

    private final EntityManager entityManager;

    /**
     * @param hastaExclusivo ya calculado en Java como hasta + 1 día, para que el rango sea inclusivo
     *                       sin depender de aritmética de fechas del motor.
     */
    public List<ResumenMovimientoStock> consultar(
            LocalDateTime desde, LocalDateTime hastaExclusivo,
            TipoArticuloStock tipoArticulo, Long idArticulo,
            TipoMovimientoStock tipoMovimiento, Long idProveedor) {

        List<String> condiciones = new ArrayList<>();
        condiciones.add("m.fecha_hora >= :desde");
        condiciones.add("m.fecha_hora < :hastaExclusivo");
        if (tipoMovimiento != null) {
            condiciones.add("m.tipo = :tipoMovimiento");
        }
        if (idArticulo != null) {
            // El id de producto y el de insumo pueden coincidir: el tipo siempre viene junto.
            condiciones.add(tipoArticulo == TipoArticuloStock.PRODUCTO
                    ? "m.id_producto = :idArticulo" : "m.id_insumo = :idArticulo");
        }
        if (idProveedor != null) {
            // EXISTS evita duplicar el movimiento cuando el insumo tiene varios proveedores.
            // Los productos no tienen proveedor y quedan fuera automáticamente.
            condiciones.add("EXISTS (SELECT 1 FROM Proveedor_Insumo pi "
                    + "WHERE pi.id_insumo = m.id_insumo AND pi.id_proveedor = :idProveedor)");
        }

        String sql = """
                SELECT CASE WHEN m.id_producto IS NOT NULL THEN 'PRODUCTO' ELSE 'INSUMO' END AS tipo,
                       COALESCE(m.id_producto, m.id_insumo) AS id_articulo,
                       COALESCE(p.nombre, i.nombre) AS nombre,
                       i.unidad_medida AS unidad,
                       COALESCE(SUM(CASE WHEN m.tipo = 'ENTRADA' THEN m.cantidad ELSE 0 END), 0)
                           AS entradas,
                       COALESCE(SUM(CASE WHEN m.tipo = 'SALIDA' AND m.id_venta IS NOT NULL
                           THEN m.cantidad ELSE 0 END), 0) AS salidas_venta,
                       COALESCE(SUM(CASE WHEN m.tipo = 'SALIDA' AND m.id_venta IS NULL
                           AND m.motivo = 'MERMA' THEN m.cantidad ELSE 0 END), 0) AS descartes,
                       COALESCE(SUM(CASE WHEN m.tipo = 'SALIDA' AND m.id_venta IS NULL
                           AND m.motivo = 'AJUSTE' THEN m.cantidad ELSE 0 END), 0) AS ajustes,
                       COALESCE(SUM(CASE WHEN m.tipo = 'SALIDA' AND m.id_venta IS NULL
                           AND m.motivo NOT IN ('MERMA', 'AJUSTE') THEN m.cantidad ELSE 0 END), 0)
                           AS otras_salidas,
                       COUNT(*) AS movimientos
                FROM Movimiento_Stock m
                LEFT JOIN Producto p ON p.id_producto = m.id_producto
                LEFT JOIN Insumo i ON i.id_insumo = m.id_insumo
                WHERE """ + "\n  " + String.join("\n  AND ", condiciones)
                + "\nGROUP BY 1, 2, 3, 4\nORDER BY nombre, tipo\n";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("desde", desde);
        query.setParameter("hastaExclusivo", hastaExclusivo);
        if (tipoMovimiento != null) {
            query.setParameter("tipoMovimiento", tipoMovimiento.name());
        }
        if (idArticulo != null) {
            query.setParameter("idArticulo", idArticulo);
        }
        if (idProveedor != null) {
            query.setParameter("idProveedor", idProveedor);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> filas = query.getResultList();
        return filas.stream()
                .map(fila -> new ResumenMovimientoStock(
                        fila[0].toString(),
                        ((Number) fila[1]).longValue(),
                        fila[2] == null ? null : fila[2].toString(),
                        fila[3] == null ? null : fila[3].toString(),
                        decimal(fila[4]), decimal(fila[5]), decimal(fila[6]),
                        decimal(fila[7]), decimal(fila[8]),
                        ((Number) fila[9]).longValue()))
                .toList();
    }

    private BigDecimal decimal(Object valor) {
        return new BigDecimal(valor.toString());
    }
}

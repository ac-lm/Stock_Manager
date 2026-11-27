package com.taller.gestion.reportes.repository;

import com.taller.gestion.reportes.dto.TipoArticuloStock;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Foto de hoy del stock: se lee la columna denormalizada {@code stock_actual} de Producto e Insumo,
 * nunca una suma de {@code Movimiento_Stock}. Reportes no escribe; SQL nativo estándar que funciona
 * igual en PostgreSQL 15 y 17.
 */
@Repository
@RequiredArgsConstructor
public class ReporteStockActualRepository {

    private final EntityManager entityManager;

    /**
     * @param categoria filtro exacto sobre {@code Producto.categoria}; los insumos no tienen
     *                  categoría, por lo que se excluyen cuando hay un filtro aplicado.
     */
    public List<ResumenStockActual> consultar(TipoArticuloStock tipo, String categoria) {
        List<String> partes = new ArrayList<>();
        if (tipo == null || tipo == TipoArticuloStock.PRODUCTO) {
            partes.add("""
                    SELECT 'PRODUCTO' AS tipo, p.id_producto AS id_articulo, p.nombre,
                           p.categoria AS categoria,
                           CAST(NULL AS varchar(50)) AS unidad,
                           COALESCE(p.stock_actual, 0) AS stock_actual,
                           CAST(NULL AS integer) AS stock_minimo
                    FROM Producto p
                    """ + (categoria == null ? "" : "WHERE p.categoria = :categoria\n"));
        }
        if (tipo == null || tipo == TipoArticuloStock.INSUMO) {
            // Los insumos no tienen categoría: con un filtro de categoría no pueden coincidir.
            partes.add("""
                    SELECT 'INSUMO' AS tipo, i.id_insumo AS id_articulo, i.nombre,
                           CAST(NULL AS varchar(50)) AS categoria,
                           i.unidad_medida AS unidad,
                           COALESCE(i.stock_actual, 0) AS stock_actual,
                           COALESCE(i.stock_minimo, 0) AS stock_minimo
                    FROM Insumo i
                    """ + (categoria == null ? "" : "WHERE 1 = 0\n"));
        }

        Query query = entityManager.createNativeQuery(
                String.join(" UNION ALL ", partes) + "ORDER BY tipo, nombre\n");
        if (categoria != null) {
            query.setParameter("categoria", categoria);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> filas = query.getResultList();
        return filas.stream()
                .map(fila -> new ResumenStockActual(
                        ((Number) fila[1]).longValue(),
                        fila[0] == null ? null : fila[0].toString(),
                        fila[2] == null ? null : fila[2].toString(),
                        fila[3] == null ? null : fila[3].toString(),
                        fila[4] == null ? null : fila[4].toString(),
                        new BigDecimal(fila[5].toString()),
                        fila[6] == null ? null : ((Number) fila[6]).intValue()))
                .toList();
    }
}

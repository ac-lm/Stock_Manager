package com.taller.gestion.stock.repository;

import com.taller.gestion.stock.entity.MotivoMovimientoStock;
import com.taller.gestion.stock.entity.MovimientoStock;
import com.taller.gestion.stock.entity.TipoMovimientoStock;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public interface MovimientoStockRepository
        extends JpaRepository<MovimientoStock, Long>, JpaSpecificationExecutor<MovimientoStock> {

    @Query("""
            SELECT m FROM MovimientoStock m
            WHERE m.insumo.id_insumo = :id_insumo
            ORDER BY m.fechaHora DESC, m.id_movimiento_stock DESC
            """)
    List<MovimientoStock> listarPorInsumo(@Param("id_insumo") Long id_insumo);

    @Query("""
            SELECT m FROM MovimientoStock m
            WHERE m.producto.id_producto = :id_producto
            ORDER BY m.fechaHora DESC, m.id_movimiento_stock DESC
            """)
    List<MovimientoStock> listarPorProducto(@Param("id_producto") Long id_producto);

    /**
     * Los filtros se arman con Criteria en vez de JPQL con parámetros opcionales: cuando un
     * parámetro llega null, PostgreSQL no puede inferir su tipo en {@code :param IS NULL} y falla
     * con "could not determine data type of parameter". Acá simplemente no se agrega el predicado.
     * El orden por defecto del historial es el más reciente primero; la paginación puede
     * cambiarlo con el parámetro {@code sort} de Spring Data.
     */
    default Page<MovimientoStock> listar(
            TipoMovimientoStock tipo, MotivoMovimientoStock motivo, Long id_producto,
            Long id_insumo, Long id_usuario, LocalDateTime desde, LocalDateTime hasta,
            Pageable pageable) {
        Specification<MovimientoStock> filtros = (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (tipo != null) {
                predicados.add(cb.equal(root.get("tipo"), tipo));
            }
            if (motivo != null) {
                predicados.add(cb.equal(root.get("motivo"), motivo));
            }
            if (id_producto != null) {
                predicados.add(cb.equal(root.get("producto").get("id_producto"), id_producto));
            }
            if (id_insumo != null) {
                predicados.add(cb.equal(root.get("insumo").get("id_insumo"), id_insumo));
            }
            if (id_usuario != null) {
                predicados.add(cb.equal(root.get("usuario").get("id_usuario"), id_usuario));
            }
            if (desde != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("fechaHora"), desde));
            }
            if (hasta != null) {
                predicados.add(cb.lessThanOrEqualTo(root.get("fechaHora"), hasta));
            }
            return cb.and(predicados.toArray(Predicate[]::new));
        };
        return findAll(filtros, pageable);
    }
}

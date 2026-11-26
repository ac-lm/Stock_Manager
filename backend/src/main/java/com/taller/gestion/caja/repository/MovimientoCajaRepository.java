package com.taller.gestion.caja.repository;

import com.taller.gestion.caja.entity.MedioPago;
import com.taller.gestion.caja.entity.MovimientoCaja;
import com.taller.gestion.caja.entity.TipoMovimientoCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface MovimientoCajaRepository extends JpaRepository<MovimientoCaja, Long> {

    @Query("""
            SELECT m FROM MovimientoCaja m
            WHERE m.sesionCaja.id_sesion = :id_sesion
            ORDER BY m.fecha DESC, m.hora DESC, m.id_movimiento_caja DESC
            """)
    List<MovimientoCaja> listarPorSesion(@Param("id_sesion") Long id_sesion);

    /** Pagos de una venta. Se filtra por tipo para no mezclar contramovimientos de CAJA-07. */
    @Query("""
            SELECT m FROM MovimientoCaja m
            WHERE m.id_venta = :id_venta AND m.tipo = :tipo
            ORDER BY m.id_movimiento_caja ASC
            """)
    List<MovimientoCaja> listarPorVentaYTipo(
            @Param("id_venta") Long id_venta,
            @Param("tipo") TipoMovimientoCaja tipo);

    @Query("""
            SELECT COALESCE(SUM(m.monto), 0)
            FROM MovimientoCaja m
            WHERE m.sesionCaja.id_sesion = :id_sesion
              AND m.medioPago = :medioPago
              AND m.tipo = :tipo
            """)
    BigDecimal sumarPorTipoYMedio(
            @Param("id_sesion") Long id_sesion,
            @Param("medioPago") MedioPago medioPago,
            @Param("tipo") TipoMovimientoCaja tipo);

    /** El vínculo se guarda en egresos; se consulta por id para CST-05. */
    @Query("""
            SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END
            FROM MovimientoCaja m
            WHERE m.id_costo = :id_costo
            """)
    boolean existePorCosto(@Param("id_costo") Long id_costo);
}

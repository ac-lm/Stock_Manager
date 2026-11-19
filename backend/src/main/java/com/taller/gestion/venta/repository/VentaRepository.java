package com.taller.gestion.venta.repository;

import com.taller.gestion.venta.entity.EstadoVenta;
import com.taller.gestion.venta.entity.Venta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    @Query("""
            SELECT v FROM Venta v
            WHERE (:estado IS NULL OR v.estado = :estado)
              AND (:id_cliente IS NULL OR v.id_cliente = :id_cliente)
              AND (:desde IS NULL OR v.fecha >= :desde)
              AND (:hasta IS NULL OR v.fecha <= :hasta)
            """)
    Page<Venta> buscar(
            @Param("estado") EstadoVenta estado,
            @Param("id_cliente") Long id_cliente,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            Pageable pageable);
}

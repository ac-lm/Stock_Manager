package com.taller.gestion.incidencias.repository;

import com.taller.gestion.incidencias.entity.EstadoIncidencia;
import com.taller.gestion.incidencias.entity.Incidencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface IncidenciaRepository extends JpaRepository<Incidencia, Long> {

    @Query("""
            SELECT i FROM Incidencia i
            WHERE (:estado IS NULL OR i.estado = :estado)
              AND (:id_tipo_incidencia IS NULL OR i.tipoIncidencia.id_tipo_incidencia = :id_tipo_incidencia)
              AND (:id_insumo IS NULL OR i.insumo.id_insumo = :id_insumo)
              AND (:id_producto IS NULL OR i.producto.id_producto = :id_producto)
              AND (:desde IS NULL OR i.fecha >= :desde)
              AND (:hasta IS NULL OR i.fecha <= :hasta)
            """)
    Page<Incidencia> buscar(
            @Param("estado") EstadoIncidencia estado,
            @Param("id_tipo_incidencia") Long id_tipo_incidencia,
            @Param("id_insumo") Long id_insumo,
            @Param("id_producto") Long id_producto,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable);
}

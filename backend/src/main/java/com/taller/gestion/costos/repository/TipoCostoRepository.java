package com.taller.gestion.costos.repository;

import com.taller.gestion.costos.entity.TipoCosto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TipoCostoRepository extends JpaRepository<TipoCosto, Long> {
    @Query("SELECT COUNT(t) > 0 FROM TipoCosto t WHERE t.nombreNormalizado = :nombre")
    boolean existsByNombreNormalizado(@Param("nombre") String nombre);

    @Query("SELECT COUNT(t) > 0 FROM TipoCosto t "
            + "WHERE t.nombreNormalizado = :nombre AND t.id_tipo_costo <> :id_tipo_costo")
    boolean existsByNombreNormalizadoAndIdNot(
            @Param("nombre") String nombre,
            @Param("id_tipo_costo") Long id_tipo_costo);

    @Query("SELECT t FROM TipoCosto t WHERE (:incluirInactivos = true OR t.activo = true) ORDER BY t.nombre")
    List<TipoCosto> listar(@Param("incluirInactivos") boolean incluirInactivos);
}

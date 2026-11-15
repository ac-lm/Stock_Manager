package com.taller.gestion.incidencias.repository;

import com.taller.gestion.incidencias.entity.TipoIncidencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TipoIncidenciaRepository extends JpaRepository<TipoIncidencia, Long> {

    List<TipoIncidencia> findAllByOrderByNombreAsc();

    @Query("""
            SELECT COUNT(t) > 0 FROM TipoIncidencia t
            WHERE LOWER(t.nombre) = LOWER(:nombre)
              AND (:id IS NULL OR t.id_tipo_incidencia <> :id)
            """)
    boolean existeNombre(@Param("nombre") String nombre, @Param("id") Long id);
}

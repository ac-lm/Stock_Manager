package com.taller.gestion.costos.repository;

import com.taller.gestion.costos.entity.Costo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface CostoRepository extends JpaRepository<Costo, Long> {
    @Query("""
        SELECT c FROM Costo c
        WHERE (:incluirAnulados = true OR c.anulado = false)
          AND (:id_tipo_costo IS NULL OR c.tipoCosto.id_tipo_costo = :id_tipo_costo)
          AND (:mes IS NULL OR c.periodoMes = :mes)
          AND (:anio IS NULL OR c.periodoAnio = :anio)
        """)
    Page<Costo> buscar(@Param("id_tipo_costo") Long id_tipo_costo,
                       @Param("mes") Integer mes,
                       @Param("anio") Integer anio,
                       @Param("incluirAnulados") boolean incluirAnulados,
                       Pageable pageable);

    // El total nunca suma anulados, aunque se los esté mostrando
    @Query("""
        SELECT COALESCE(SUM(c.monto), 0) FROM Costo c
        WHERE c.anulado = false
          AND (:id_tipo_costo IS NULL OR c.tipoCosto.id_tipo_costo = :id_tipo_costo)
          AND (:mes IS NULL OR c.periodoMes = :mes)
          AND (:anio IS NULL OR c.periodoAnio = :anio)
        """)
    BigDecimal total(@Param("id_tipo_costo") Long id_tipo_costo,
                     @Param("mes") Integer mes,
                     @Param("anio") Integer anio);
}
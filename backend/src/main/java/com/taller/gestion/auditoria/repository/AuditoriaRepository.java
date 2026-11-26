package com.taller.gestion.auditoria.repository;

import com.taller.gestion.auditoria.entity.AccionAuditoria;
import com.taller.gestion.auditoria.entity.Auditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    LocalDateTime DESDE_INICIO = LocalDateTime.of(1900, 1, 1, 0, 0);
    LocalDateTime HASTA_FIN = LocalDateTime.of(9999, 12, 31, 23, 59, 59);

    /**
     * Los filtros nulos de fecha se reemplazan por límites seguros: en Postgres un parámetro
     * {@code null} comparado con {@code timestamp} no tiene tipo determinado y falla la query.
     */
    default Page<Auditoria> buscar(Long id_usuario, AccionAuditoria accion, String tabla,
                                   LocalDateTime desde, LocalDateTime hasta, Pageable pageable) {
        // Un parámetro String null se enlaza como bytea en Postgres y rompe el LOWER.
        String filtroTabla = tabla == null ? "" : tabla.trim();
        return buscarEntre(id_usuario, accion, filtroTabla,
                desde == null ? DESDE_INICIO : desde,
                hasta == null ? HASTA_FIN : hasta,
                pageable);
    }

    /**
     * Consulta con filtros opcionales. El orden fijo es el más reciente primero: los IDs con
     * guion bajo no se pueden usar en el {@code Pageable} de Spring Data porque se interpretan
     * como rutas anidadas.
     */
    @Query("""
            SELECT a FROM Auditoria a
            WHERE (:id_usuario IS NULL OR a.id_usuario = :id_usuario)
              AND (:accion IS NULL OR a.accion = :accion)
              AND LOWER(COALESCE(a.tabla, '')) LIKE LOWER(CONCAT('%', :tabla, '%'))
              AND a.fecha_hora >= :desde
              AND a.fecha_hora <= :hasta
            ORDER BY a.fecha_hora DESC, a.id_auditoria DESC
            """)
    Page<Auditoria> buscarEntre(@Param("id_usuario") Long id_usuario,
                                @Param("accion") AccionAuditoria accion,
                                @Param("tabla") String tabla,
                                @Param("desde") LocalDateTime desde,
                                @Param("hasta") LocalDateTime hasta,
                                Pageable pageable);
}

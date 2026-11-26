package com.taller.gestion.caja.repository;

import com.taller.gestion.caja.entity.EstadoSesionCaja;
import com.taller.gestion.caja.entity.SesionCaja;
import jakarta.persistence.LockModeType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SesionCajaRepository
        extends JpaRepository<SesionCaja, Long>, JpaSpecificationExecutor<SesionCaja> {

    Optional<SesionCaja> findFirstByEstadoOrderByFechaAperturaDescHoraAperturaDesc(
            EstadoSesionCaja estado);

    /**
     * Último cierre entre las sesiones cerradas o pendientes de revisión. Los cierres sin fecha
     * no cuentan: en PostgreSQL un ORDER BY DESC deja los NULL primero si no se aclara.
     */
    @Query("SELECT s FROM SesionCaja s WHERE s.estado IN :estados " +
            "ORDER BY s.fechaCierre DESC NULLS LAST, s.horaCierre DESC NULLS LAST")
    List<SesionCaja> buscarPorCierreDescendente(
            @Param("estados") Collection<EstadoSesionCaja> estados, Pageable pageable);

    default Optional<SesionCaja> buscarUltimoCierre(Collection<EstadoSesionCaja> estados) {
        return buscarPorCierreDescendente(estados, PageRequest.of(0, 1))
                .stream()
                .findFirst();
    }

    /**
     * Bloquea la fila de la sesión durante la transacción. Se usa para cerrar y revisar sin
     * que dos operaciones concurrentes pisen el mismo estado.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SesionCaja s WHERE s.id_sesion = :id_sesion")
    Optional<SesionCaja> findByIdForUpdate(@Param("id_sesion") Long id_sesion);

    /**
     * Los filtros se arman con Criteria para no enviar parámetros nulos a PostgreSQL.
     * El filtro por usuario incluye tanto al que abrió como al que cerró.
     */
    default Page<SesionCaja> buscar(
            EstadoSesionCaja estado,
            Long id_usuario,
            LocalDate desde,
            LocalDate hasta,
            Pageable pageable) {

        Specification<SesionCaja> filtros = (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (estado != null) {
                predicados.add(cb.equal(root.get("estado"), estado));
            }
            if (desde != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("fechaApertura"), desde));
            }
            if (hasta != null) {
                predicados.add(cb.lessThanOrEqualTo(root.get("fechaApertura"), hasta));
            }
            if (id_usuario != null) {
                predicados.add(cb.or(
                        cb.equal(root.get("usuarioApertura").get("id_usuario"), id_usuario),
                        cb.equal(root.get("usuarioCierre").get("id_usuario"), id_usuario)));
            }
            return cb.and(predicados.toArray(Predicate[]::new));
        };

        return findAll(filtros, pageable);
    }
}

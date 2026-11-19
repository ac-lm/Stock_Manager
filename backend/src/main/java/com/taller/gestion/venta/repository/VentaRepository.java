package com.taller.gestion.venta.repository;

import com.taller.gestion.venta.entity.EstadoVenta;
import com.taller.gestion.venta.entity.Venta;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long>, JpaSpecificationExecutor<Venta> {

    /**
     * Los filtros se arman con Criteria en vez de JPQL con parámetros opcionales: cuando un
     * parámetro LocalDate llega null, PostgreSQL no puede inferir el tipo y falla con
     * "cannot cast type bytea to date". Acá simplemente no se agrega ese predicado.
     */
    default Page<Venta> buscar(
            EstadoVenta estado, Long id_cliente, LocalDate desde, LocalDate hasta, Pageable pageable) {
        Specification<Venta> filtros = (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (estado != null) {
                predicados.add(cb.equal(root.get("estado"), estado));
            }
            if (id_cliente != null) {
                predicados.add(cb.equal(root.get("id_cliente"), id_cliente));
            }
            if (desde != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("fecha"), desde));
            }
            if (hasta != null) {
                predicados.add(cb.lessThanOrEqualTo(root.get("fecha"), hasta));
            }
            return cb.and(predicados.toArray(Predicate[]::new));
        };
        return findAll(filtros, pageable);
    }
}

package com.taller.gestion.catalogo.repository;

import com.taller.gestion.catalogo.entity.Insumo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InsumoRepository extends JpaRepository<Insumo, Long> {

    List<Insumo> findAllByOrderByNombreAsc();

    /** Bloquea la fila mientras se aplica un movimiento de stock. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Insumo i WHERE i.id_insumo = :id_insumo")
    Optional<Insumo> findByIdParaActualizar(@Param("id_insumo") Long id_insumo);
}

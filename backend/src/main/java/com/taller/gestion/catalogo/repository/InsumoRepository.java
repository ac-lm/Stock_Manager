package com.taller.gestion.catalogo.repository;

import com.taller.gestion.catalogo.entity.Insumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InsumoRepository extends JpaRepository<Insumo, Long> {

    List<Insumo> findAllByOrderByNombreAsc();
}

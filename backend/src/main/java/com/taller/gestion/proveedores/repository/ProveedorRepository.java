package com.taller.gestion.proveedores.repository;

import com.taller.gestion.proveedores.entity.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    List<Proveedor> findAllByOrderByNombreAsc();

    boolean existsByCuit(String cuit);
}

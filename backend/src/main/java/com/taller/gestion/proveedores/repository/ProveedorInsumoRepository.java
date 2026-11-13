package com.taller.gestion.proveedores.repository;

import com.taller.gestion.proveedores.entity.ProveedorInsumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProveedorInsumoRepository extends JpaRepository<ProveedorInsumo, Long> {

    // Se usa @Query porque los atributos de las entidades llevan guion bajo
    // (id_proveedor, id_insumo) y Spring Data los interpretaría como rutas anidadas.
    @Query("""
            SELECT pi FROM ProveedorInsumo pi
            WHERE pi.proveedor.id_proveedor = :id_proveedor
            ORDER BY pi.insumo.nombre ASC
            """)
    List<ProveedorInsumo> findPorProveedor(@Param("id_proveedor") Long id_proveedor);

    @Query("""
            SELECT pi FROM ProveedorInsumo pi
            WHERE pi.proveedor.id_proveedor = :id_proveedor
              AND pi.insumo.id_insumo = :id_insumo
            """)
    Optional<ProveedorInsumo> buscarPorProveedorEInsumo(
            @Param("id_proveedor") Long id_proveedor,
            @Param("id_insumo") Long id_insumo);
}

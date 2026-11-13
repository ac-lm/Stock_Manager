package com.taller.gestion.catalogo.repository;

import com.taller.gestion.catalogo.entity.ProductoInsumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoInsumoRepository extends JpaRepository<ProductoInsumo, Long> {

    // Se usa @Query porque los atributos de las entidades llevan guion bajo
    // (id_producto, id_insumo) y Spring Data los interpretaría como rutas anidadas.
    @Query("""
            SELECT pi FROM ProductoInsumo pi
            WHERE pi.producto.id_producto = :id_producto
            ORDER BY pi.insumo.nombre ASC
            """)
    List<ProductoInsumo> findPorProducto(@Param("id_producto") Long id_producto);

    @Query("""
            SELECT pi FROM ProductoInsumo pi
            WHERE pi.producto.id_producto = :id_producto
              AND pi.insumo.id_insumo = :id_insumo
            """)
    Optional<ProductoInsumo> buscarPorProductoEInsumo(
            @Param("id_producto") Long id_producto,
            @Param("id_insumo") Long id_insumo);
}

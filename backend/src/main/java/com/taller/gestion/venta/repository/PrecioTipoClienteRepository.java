package com.taller.gestion.venta.repository;

import com.taller.gestion.clientes.entity.TipoCliente;
import com.taller.gestion.venta.entity.PrecioTipoCliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrecioTipoClienteRepository extends JpaRepository<PrecioTipoCliente, Long> {

    /**
     * Se pone @Query porque el campo de la entidad es id_producto (nomenclatura del modelo) y
     * el derivado de Spring Data esperaría idProducto. Con JPQL se usa el nombre real del campo.
     */
    @Query("""
            SELECT p FROM PrecioTipoCliente p
            WHERE p.id_producto = :id_producto
              AND p.tipoCliente = :tipoCliente
            """)
    Optional<PrecioTipoCliente> findByIdProductoAndTipoCliente(
            @Param("id_producto") Long id_producto,
            @Param("tipoCliente") TipoCliente tipoCliente);
}

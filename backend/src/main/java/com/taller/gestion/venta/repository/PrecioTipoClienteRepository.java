package com.taller.gestion.venta.repository;

import com.taller.gestion.clientes.entity.TipoCliente;
import com.taller.gestion.venta.entity.PrecioTipoCliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrecioTipoClienteRepository extends JpaRepository<PrecioTipoCliente, Long> {

    Optional<PrecioTipoCliente> findByIdProductoAndTipoCliente(Long id_producto, TipoCliente tipoCliente);
}

package com.taller.gestion.clientes.repository;

import com.taller.gestion.clientes.entity.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    boolean existsByCuit(String cuit);

    @Query("""
    SELECT c FROM Cliente c
    WHERE (:incluirInactivos = true OR c.activo = true)
      AND (
            :busqueda = ''
            OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%'))
            OR REPLACE(c.cuit, '-', '') LIKE CONCAT('%', :digitos, '%')
          )
    """)
    Page<Cliente> buscar(@Param("busqueda") String busqueda,
                         @Param("digitos") String digitos,
                         @Param("incluirInactivos") boolean incluirInactivos,
                         Pageable pageable);

    @Query("SELECT COUNT(c) > 0 FROM Cliente c "
            + "WHERE c.cuit = :cuit AND c.id_cliente <> :id_cliente")
    boolean existsByCuitAndIdNot(@Param("cuit") String cuit,
                                 @Param("id_cliente") Long id_cliente);

    @Query("""
    SELECT c FROM Cliente c
    WHERE c.tipoCliente <> com.taller.gestion.clientes.entity.TipoCliente.GENERICO
      AND c.nombreNormalizado LIKE CONCAT('%', :texto, '%')
    ORDER BY c.nombre
    """)
    List<Cliente> buscarSimilares(@Param("texto") String texto, Pageable pageable);
}

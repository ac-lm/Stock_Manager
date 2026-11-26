package com.taller.gestion.caja.repository;

import com.taller.gestion.caja.entity.CierreCajaDetalle;
import com.taller.gestion.caja.entity.CierreCajaDetalleId;
import com.taller.gestion.caja.entity.MedioPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CierreCajaDetalleRepository
        extends JpaRepository<CierreCajaDetalle, CierreCajaDetalleId> {

    @Query("""
            SELECT d FROM CierreCajaDetalle d
            WHERE d.id.id_sesion = :id_sesion
            ORDER BY d.id.medioPago
            """)
    List<CierreCajaDetalle> listarPorSesion(@Param("id_sesion") Long id_sesion);

    @Query("""
            SELECT d FROM CierreCajaDetalle d
            WHERE d.id.id_sesion = :id_sesion
              AND d.id.medioPago = :medioPago
            """)
    Optional<CierreCajaDetalle> buscarPorSesionYMedio(
            @Param("id_sesion") Long id_sesion,
            @Param("medioPago") MedioPago medioPago);
}

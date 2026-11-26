package com.taller.gestion.caja.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Lo esperado, lo declarado y la diferencia de un cierre para un medio de pago.
 * La clave es ({@code id_sesion}, {@code medio_pago}).
 */
@Entity
@Table(name = "Cierre_Caja_Detalle")
@Getter
@Setter
@NoArgsConstructor
public class CierreCajaDetalle {

    @EmbeddedId
    private CierreCajaDetalleId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("id_sesion")
    @JoinColumn(name = "id_sesion", nullable = false)
    private SesionCaja sesionCaja;

    @Column(name = "total_esperado", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalEsperado;

    @Column(name = "total_declarado", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalDeclarado;

    @Column(name = "diferencia", precision = 12, scale = 2, nullable = false)
    private BigDecimal diferencia;
}

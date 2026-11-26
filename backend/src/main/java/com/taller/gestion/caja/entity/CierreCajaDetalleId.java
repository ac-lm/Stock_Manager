package com.taller.gestion.caja.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Clave compuesta del detalle del cierre: una fila por sesión y medio de pago.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CierreCajaDetalleId implements Serializable {

    @Column(name = "id_sesion", nullable = false)
    private Long id_sesion;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio_pago", length = 20, nullable = false)
    private MedioPago medioPago;
}

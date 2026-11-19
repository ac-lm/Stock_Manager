package com.taller.gestion.venta.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "Detalle_Venta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DetalleVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle_venta")
    private Long id_detalle_venta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_venta_fk", nullable = false)
    private Venta venta;

    // FK a Producto. Se valida desde ProductoService, no se acopla la entidad al otro módulo.
    @Column(name = "id_producto_fk", nullable = false)
    private Long id_producto_fk;

    @Column(name = "cantidad", precision = 12, scale = 3, nullable = false)
    private BigDecimal cantidad;

    /** Precio de la lista correspondiente al tipo de cliente al momento de la venta. */
    @Column(name = "precio_historico", precision = 12, scale = 2, nullable = false)
    private BigDecimal precioHistorico;

    @Column(name = "subtotal", precision = 12, scale = 2, nullable = false)
    private BigDecimal subtotal;
}

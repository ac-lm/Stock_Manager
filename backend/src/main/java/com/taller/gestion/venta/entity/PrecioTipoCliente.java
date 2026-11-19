package com.taller.gestion.venta.entity;

import com.taller.gestion.clientes.entity.TipoCliente;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Lista de precios por tipo de cliente (decisión 2b del relevamiento).
 *
 * <p>Todavía no está en el export de Vertabelo/Redgate: se crea desde JPA con
 * ddl-auto=update y hay que agregarla al modelo con estas mismas columnas. Mientras no haya
 * filas cargadas, las ventas usan Producto.precio como precio de catálogo.
 */
@Entity
@Table(name = "Precio_Tipo_Cliente", uniqueConstraints = @UniqueConstraint(
        name = "uk_precio_tipo_cliente",
        columnNames = {"id_producto", "tipo_cliente"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrecioTipoCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_precio_tipo_cliente")
    private Long id_precio_tipo_cliente;

    @Column(name = "id_producto", nullable = false)
    private Long id_producto;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_cliente", length = 20, nullable = false)
    private TipoCliente tipoCliente;

    @Column(name = "precio", precision = 12, scale = 2, nullable = false)
    private BigDecimal precio;
}

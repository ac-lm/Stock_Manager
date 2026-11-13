package com.taller.gestion.catalogo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Producto")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Long id_producto;

    @Column(name = "nombre", length = 50, nullable = false)
    private String nombre;

    @Column(name = "precio", precision = 12, scale = 2, nullable = false)
    private BigDecimal precio;

    @Column(name = "stock_actual", precision = 12, scale = 3, nullable = false)
    private BigDecimal stockActual;

    @Column(name = "categoria", length = 50, nullable = false)
    private String categoria;

    // Al eliminar el producto se eliminan sus relaciones con insumos.
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductoInsumo> insumos = new ArrayList<>();
}

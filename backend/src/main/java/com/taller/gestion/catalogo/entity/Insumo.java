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
@Table(name = "Insumo")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Insumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_insumo")
    private Long id_insumo;

    @Column(name = "nombre", length = 50, nullable = false)
    private String nombre;

    @Column(name = "stock_actual", nullable = false)
    private Integer stockActual;

    @Column(name = "unidad_medida", length = 50, nullable = false)
    private String unidadMedida;

    @Column(name = "stock_minimo", nullable = false)
    private Integer stockMinimo;

    @Column(name = "costo", precision = 12, scale = 2, nullable = false)
    private BigDecimal costo;

    // Al eliminar el insumo se eliminan sus relaciones con productos.
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "insumo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductoInsumo> productos = new ArrayList<>();
}

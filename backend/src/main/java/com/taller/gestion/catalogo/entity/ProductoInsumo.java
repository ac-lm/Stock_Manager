package com.taller.gestion.catalogo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Producto_Insumo")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoInsumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto_insumo")
    private Long id_producto_insumo;

    // Relación con Producto (Módulo Catálogo)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    // Relación con Insumo (Módulo Catálogo)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_insumo", nullable = false)
    private Insumo insumo;

    @Column(name = "cant_necesaria", nullable = false)
    private Float cantNecesaria;

    @Column(name = "unidad_medida", length = 50, nullable = false)
    private String unidadMedida;
}

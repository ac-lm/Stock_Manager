package com.taller.gestion.proveedores.entity;

import com.taller.gestion.catalogo.entity.Insumo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Proveedor_Insumo")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProveedorInsumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proveedor_insumo")
    private Long id_proveedor_insumo;

    // Relación con Proveedor (Módulo Proveedores)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proveedor", nullable = false)
    private Proveedor proveedor;

    // Relación con Insumo (Módulo Catálogo)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_insumo", nullable = false)
    private Insumo insumo;
}

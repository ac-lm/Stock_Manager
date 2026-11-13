package com.taller.gestion.proveedores.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Proveedor")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proveedor")
    private Long id_proveedor;

    @Column(name = "nombre", length = 50, nullable = false)
    private String nombre;

    @Column(name = "CUIT", length = 13, nullable = false, unique = true)
    private String cuit;

    @Column(name = "telefono", length = 50, nullable = false)
    private String telefono;

    @Column(name = "email", length = 50)
    private String email;

    @Column(name = "tipo_producto", length = 50, nullable = false)
    private String tipo_producto;

    @Column(name = "observaciones", length = 50)
    private String observaciones;

    // Al eliminar el proveedor se eliminan sus relaciones con insumos.
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "proveedor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProveedorInsumo> insumos = new ArrayList<>();
}

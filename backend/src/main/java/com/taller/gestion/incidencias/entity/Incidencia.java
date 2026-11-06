package com.taller.gestion.incidencias.entity;

import com.taller.gestion.catalogo.entity.Insumo;
// import com.taller.gestion.catalogo.entity.Producto; // Descomentar cuando exista Producto
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Incidencia")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Incidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_incidencia")
    private Integer idIncidencia;

    // Relación con Insumo (Módulo Catálogo)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_insumo")
    private Insumo insumo;

    // Relación con Producto (Módulo Catálogo) - Descomentar cuando Producto esté creado
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "id_producto")
    // private Producto producto;

    @Column(name = "tipo_incidencia", length = 50, nullable = false)
    private String tipoIncidencia;

    @Column(name = "descripcion", length = 100)
    private String descripcion;

    @Column(name = "cantidad_afectada")
    private Integer cantidadAfectada;

    @Column(name = "estado", length = 50)
    private String estado;

    @Column(name = "observaciones", length = 100)
    private String observaciones;

    @Column(name = "resolucion_adoptada", length = 100)
    private String resolucionAdoptada;
}
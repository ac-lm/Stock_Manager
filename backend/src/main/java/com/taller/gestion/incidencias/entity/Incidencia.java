package com.taller.gestion.incidencias.entity;

import com.taller.gestion.catalogo.entity.Insumo;
import com.taller.gestion.catalogo.entity.Producto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Incidencia")
@Check(constraints = "id_producto IS NOT NULL OR id_insumo IS NOT NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Incidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_incidencia")
    private Long id_incidencia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tipo_incidencia", nullable = false)
    private TipoIncidencia tipoIncidencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_insumo")
    private Insumo insumo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto")
    private Producto producto;

    @Column(name = "descripcion", length = 100)
    private String descripcion;

    @Column(name = "cantidad_afectada", precision = 12, scale = 3, nullable = false)
    private BigDecimal cantidadAfectada;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    private EstadoIncidencia estado;

    @Column(name = "observaciones", length = 100)
    private String observaciones;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    // Temporal: sin FK hasta integrar el módulo usuario / login.
    @Column(name = "id_usuario")
    private Long id_usuario;

    // Temporal: sin FK hasta integrar el módulo usuario / login.
    @Column(name = "id_usuario_resolucion")
    private Long id_usuario_resolucion;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    @Column(name = "resolucion_adoptada", length = 100)
    private String resolucionAdoptada;

    @PrePersist
    private void precargarValores() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
        if (estado == null) {
            estado = EstadoIncidencia.ABIERTA;
        }
    }
}

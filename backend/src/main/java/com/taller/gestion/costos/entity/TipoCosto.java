package com.taller.gestion.costos.entity;

import com.taller.gestion.util.TextoUtil;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "TipoCosto")
@Getter
@Setter
@NoArgsConstructor
public class TipoCosto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_costo")
    private Long id_tipo_costo;

    @Column(name = "nombre", length = 50, nullable = false)
    private String nombre;

    @Column(name = "nombre_normalizado", length = 50, nullable = false, unique = true)
    private String nombreNormalizado;

    @Column(nullable = false)
    private boolean activo = true;

    // true = lo maneja el sistema (hoy solo "Insumos"); no se edita ni se desactiva
    @Column(nullable = false)
    private boolean reservado = false;

    @PrePersist
    @PreUpdate
    private void actualizarNombreNormalizado() {
        this.nombreNormalizado = TextoUtil.normalizarParaBusqueda(this.nombre);
    }
}

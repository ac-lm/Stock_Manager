package com.taller.gestion.incidencias.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Tipo_Incidencia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TipoIncidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_incidencia")
    private Long id_tipo_incidencia;

    @Column(name = "nombre", length = 50, nullable = false, unique = true)
    private String nombre;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;
}

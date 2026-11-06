package com.taller.gestion.catalogo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "Insumo")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Insumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_insumo")
    private Integer idInsumo;

    @Column(name = "nombre", length = 50, nullable = false)
    private String nombre;

    @Column(name = "stock_actual")
    private Integer stockActual;

    @Column(name = "unidad_medida", length = 50)
    private String unidadMedida;

    @Column(name = "stock_minimo")
    private Integer stockMinimo;

    @Column(name = "costo", precision = 12, scale = 2)
    private BigDecimal costo;
}
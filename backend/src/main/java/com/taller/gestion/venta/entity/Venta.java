package com.taller.gestion.venta.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Data
@Table(name="venta")
public class Venta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_venta;

    private Long id_cliente;

    private LocalDate fecha;

    private LocalTime hora;

    private Double total;

    private Long id_vendedor;

    private String estado;

    private Double descuento;
}

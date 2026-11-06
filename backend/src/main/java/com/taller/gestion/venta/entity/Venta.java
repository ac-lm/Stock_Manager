package com.taller.gestion.venta.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import java.time.LocalDate;

@Entity
public class Venta {
    @Id
    private Long id_venta;
    private Long id_cliente;
    private LocalDate fecha;


}

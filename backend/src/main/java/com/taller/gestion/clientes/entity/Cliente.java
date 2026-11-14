package com.taller.gestion.clientes.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Cliente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Long id_cliente;

    // puede ser nulo únicamente para un consumidor final.
    @Column(name = "CUIT", length = 13, unique = true)
    private String cuit;

    @Column(name = "nombre", length = 50, nullable = false)
    private String nombre;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "email", length = 150, nullable = false)
    private String email;

    @Column(name = "direccion", length = 80, nullable = false)
    private String direccion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoCliente tipo_cliente;

    @Column(name = "observaciones", length = 250)
    private String observaciones;

    @Column(nullable = false)
    private boolean activo = true;
}

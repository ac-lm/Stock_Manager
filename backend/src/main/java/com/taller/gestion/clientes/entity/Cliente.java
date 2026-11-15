package com.taller.gestion.clientes.entity;

import com.taller.gestion.util.TextoUtil;
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

    @Column(name = "plazo_pago_dias")
    private Integer plazo_pago_dias;

    @Column(name = "nombre_normalizado", length = 50, nullable = false)
    private String nombre_normalizado;

    @PrePersist
    @PreUpdate
    private void actualizarNombreNormalizado() {
        this.nombre_normalizado = TextoUtil.normalizarParaBusqueda(this.nombre);
    }
}

package com.taller.gestion.venta.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Venta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_venta")
    private Long id_venta;

    // FK a Cliente. Se valida desde ClienteService, no se acopla la entidad al otro módulo.
    @Column(name = "id_cliente", nullable = false)
    private Long id_cliente;

    // FK a Usuario. Siempre es el usuario autenticado que carga la venta.
    @Column(name = "id_vendedor", nullable = false)
    private Long id_vendedor;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "hora", nullable = false)
    private LocalTime hora;

    @Column(name = "total", precision = 12, scale = 2, nullable = false)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    private EstadoVenta estado;

    /**
     * Vencimiento del saldo cuando la venta viene de un pedido con seña. Opcional:
     * en una venta de mostrador queda nulo. La regla de negocio llega con VEN-09.
     */
    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleVenta> detalles = new ArrayList<>();

    public void agregarDetalle(DetalleVenta detalle) {
        detalles.add(detalle);
        detalle.setVenta(this);
    }

    @PrePersist
    private void precargarValores() {
        if (fecha == null) {
            fecha = LocalDate.now();
        }
        if (hora == null) {
            hora = LocalTime.now();
        }
        if (estado == null) {
            estado = EstadoVenta.CONFIRMADA;
        }
    }
}

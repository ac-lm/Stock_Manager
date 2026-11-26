package com.taller.gestion.stock.entity;

import com.taller.gestion.catalogo.entity.Insumo;
import com.taller.gestion.catalogo.entity.Producto;
import com.taller.gestion.usuario.entity.Usuario;
import com.taller.gestion.venta.entity.Venta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Trazabilidad de stock. Cada movimiento afecta exactamente un producto o un insumo y es
 * inmutable: las correcciones se registran con otro movimiento. {@code id_venta} e
 * {@code id_ingreso_mercaderia} quedan reservados para integraciones futuras.
 */
@Entity
@Table(name = "Movimiento_Stock")
@Getter
@Setter
@NoArgsConstructor
public class MovimientoStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento_stock")
    private Long id_movimiento_stock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto")
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_insumo")
    private Insumo insumo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 10, nullable = false)
    private TipoMovimientoStock tipo;

    @Column(name = "cantidad", precision = 12, scale = 3, nullable = false)
    private BigDecimal cantidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo", length = 30, nullable = false)
    private MotivoMovimientoStock motivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "observaciones", length = 150)
    private String observaciones;

    // Reservado para VEN-10.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_venta")
    private Venta venta;

    // Reservado para CST-06 (no existe todavía la tabla de ingreso de mercadería).
    @Column(name = "id_ingreso_mercaderia")
    private Long id_ingreso_mercaderia;
}

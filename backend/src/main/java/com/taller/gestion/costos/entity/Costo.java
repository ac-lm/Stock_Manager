package com.taller.gestion.costos.entity;

import com.taller.gestion.usuario.entity.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "Costo")
@Getter
@Setter
@NoArgsConstructor
public class Costo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_costo")
    private Long id_costo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tipo_costo", nullable = false)
    private TipoCosto tipoCosto;

    @Column(name = "descripcion", length = 255, nullable = false)
    private String descripcion;

    @Column(name = "monto", precision = 12, scale = 2, nullable = false)
    private BigDecimal monto;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDate fechaPago;

    @Column(name = "periodo_mes", nullable = false)
    private Integer periodoMes;

    @Column(name = "periodo_anio", nullable = false)
    private Integer periodoAnio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    // TODO: pasar a @ManyToOne hacia Ingreso_Mercaderia. Único: un ingreso genera un solo costo
    @Column(name = "id_ingreso_mercaderia", unique = true)
    private Long id_ingreso_mercaderia;

    @Column(nullable = false)
    private boolean anulado = false;

    @Column(name = "motivo_anulacion", length = 255)
    private String motivoAnulacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_anulacion")
    private Usuario usuarioAnulacion;

    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;

    @Column(name = "observaciones", length = 255)
    private String observaciones;
}
package com.taller.gestion.caja.entity;

import com.taller.gestion.usuario.entity.Usuario;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Una sesión de caja: el período entre una apertura y su cierre. Nunca hay dos sesiones
 * {@link EstadoSesionCaja#ABIERTA} al mismo tiempo; la unicidad se protege en el servicio
 * con un advisory lock de PostgreSQL.
 */
@Entity
@Table(name = "Sesion_Caja")
@Getter
@Setter
@NoArgsConstructor
public class SesionCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sesion")
    private Long id_sesion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario_apertura", nullable = false)
    private Usuario usuarioApertura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_cierre")
    private Usuario usuarioCierre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_revision")
    private Usuario usuarioRevision;

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDate fechaApertura;

    @Column(name = "hora_apertura", nullable = false)
    private LocalTime horaApertura;

    @Column(name = "fecha_cierre")
    private LocalDate fechaCierre;

    @Column(name = "hora_cierre")
    private LocalTime horaCierre;

    @Column(name = "monto_inicial", precision = 12, scale = 2, nullable = false)
    private BigDecimal montoInicial;

    /** Obligatorio solo si el monto inicial difiere del sugerido por el sistema. */
    @Column(name = "motivo_ajuste_inicial", length = 150)
    private String motivoAjusteInicial;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 30, nullable = false)
    private EstadoSesionCaja estado;

    @Column(name = "total_esperado", precision = 12, scale = 2)
    private BigDecimal totalEsperado;

    @Column(name = "total_declarado", precision = 12, scale = 2)
    private BigDecimal totalDeclarado;

    @Column(name = "diferencia", precision = 12, scale = 2)
    private BigDecimal diferencia;

    /** Obligatoria cuando alguna diferencia del cierre no es cero. */
    @Column(name = "observaciones", length = 100)
    private String observaciones;

    @Column(name = "fecha_revision")
    private LocalDateTime fechaRevision;

    @Column(name = "observacion_revision", length = 150)
    private String observacionRevision;
}

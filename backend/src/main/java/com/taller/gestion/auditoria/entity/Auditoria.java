package com.taller.gestion.auditoria.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Registro inmutable de una operación relevante.
 *
 * <p>La fecha y hora se guardan en una sola columna {@code timestamp}, en vez del
 * {@code fecha}/{@code hora} separado del modelo original. El usuario se guarda como id y
 * como texto: el id permite cruzar con {@code Usuario} y el texto conserva el nombre aunque
 * la cuenta se desactive o cambie de email. No se crea una FK para no depender del módulo
 * usuario y para que la evidencia sobreviva a cualquier cambio de ese módulo.</p>
 *
 * <p>{@code cambios} es un JSONB con la forma
 * {@code {"campo": {"anterior": valorViejo, "nuevo": valorNuevo}}}. Ese formato sirve para
 * cualquier tabla sin necesidad de una tabla de auditoría por entidad.</p>
 *
 * <p>Las filas nunca se editan ni se borran. Si una carga manual fue incorrecta se agrega una
 * nueva fila con {@code accion = CORRECCION_AUDITORIA} y {@code id_auditoria_corregida}
 * apuntando a la original, que queda intacta.</p>
 */
@Entity
@Table(name = "Auditoria")
@Getter
@NoArgsConstructor
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Long id_auditoria;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private LocalDateTime fecha_hora;

    /** Id del usuario que hizo la operación; null cuando la hizo el sistema. */
    @Column(name = "id_usuario", updatable = false)
    private Long id_usuario;

    /** Email del usuario (o "SISTEMA"), congelado al momento de la operación. */
    @Column(name = "usuario", length = 255, updatable = false)
    private String usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "accion", nullable = false, length = 30, updatable = false)
    private AccionAuditoria accion;

    /** Nombre de la tabla afectada, o del recurso de negocio cuando no aplica. */
    @Column(name = "tabla", length = 50, updatable = false)
    private String tabla;

    @Column(name = "id_registro", updatable = false)
    private Long id_registro;

    /** En una corrección, id de la fila de auditoría que se enmienda. */
    @Column(name = "id_auditoria_corregida", updatable = false)
    private Long id_auditoria_corregida;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cambios", columnDefinition = "jsonb", updatable = false)
    private Map<String, Object> cambios;

    /** Descripción libre para acciones de negocio (por ejemplo, un cierre de caja). */
    @Column(name = "detalle", length = 255, updatable = false)
    private String detalle;

    /** Constructor de los registros automáticos (listener e interceptor de login). */
    public Auditoria(LocalDateTime fecha_hora, Long id_usuario, String usuario,
                     AccionAuditoria accion, String tabla, Long id_registro,
                     Map<String, Object> cambios, String detalle) {
        this(fecha_hora, id_usuario, usuario, accion, tabla, id_registro, cambios, detalle, null);
    }

    /** Constructor de las cargas manuales, incluidas las correcciones. */
    public Auditoria(LocalDateTime fecha_hora, Long id_usuario, String usuario,
                     AccionAuditoria accion, String tabla, Long id_registro,
                     Map<String, Object> cambios, String detalle, Long id_auditoria_corregida) {
        this.fecha_hora = fecha_hora;
        this.id_usuario = id_usuario;
        this.usuario = usuario;
        this.accion = accion;
        this.tabla = tabla;
        this.id_registro = id_registro;
        this.id_auditoria_corregida = id_auditoria_corregida;
        this.cambios = cambios == null ? new java.util.LinkedHashMap<>() : cambios;
        this.detalle = detalle;
    }
}

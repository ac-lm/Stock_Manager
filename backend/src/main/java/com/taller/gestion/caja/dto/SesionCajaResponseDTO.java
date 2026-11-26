package com.taller.gestion.caja.dto;

import com.taller.gestion.caja.entity.EstadoSesionCaja;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Schema(description = "Sesión de caja con sus totales por medio de pago")
public record SesionCajaResponseDTO(

        @Schema(description = "Id de la sesión") Long id_sesion,
        @Schema(description = "Id del usuario que abrió") Long id_usuario_apertura,
        @Schema(description = "Nombre del usuario que abrió") String usuarioApertura,
        @Schema(description = "Fecha de apertura") LocalDate fechaApertura,
        @Schema(description = "Hora de apertura") LocalTime horaApertura,
        @Schema(description = "Id del usuario que cerró", nullable = true) Long id_usuario_cierre,
        @Schema(description = "Nombre del usuario que cerró", nullable = true) String usuarioCierre,
        @Schema(description = "Fecha de cierre", nullable = true) LocalDate fechaCierre,
        @Schema(description = "Hora de cierre", nullable = true) LocalTime horaCierre,
        @Schema(description = "Monto inicial en efectivo") BigDecimal montoInicial,
        @Schema(description = "Motivo del ajuste del monto inicial", nullable = true)
        String motivoAjusteInicial,
        @Schema(description = "ABIERTA, PENDIENTE_REVISION o CERRADA") EstadoSesionCaja estado,
        @Schema(description = "Totales por medio de pago") TotalesCajaDTO totales,
        @Schema(description = "Observación del cierre", nullable = true) String observaciones,
        @Schema(description = "Id del administrador que revisó", nullable = true)
        Long id_usuario_revision,
        @Schema(description = "Nombre del administrador que revisó", nullable = true)
        String usuarioRevision,
        @Schema(description = "Fecha y hora de la revisión", nullable = true)
        LocalDateTime fechaRevision,
        @Schema(description = "Observación de la revisión", nullable = true)
        String observacionRevision
) {
}

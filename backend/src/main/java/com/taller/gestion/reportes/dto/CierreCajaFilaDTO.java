package com.taller.gestion.reportes.dto;

import com.taller.gestion.caja.entity.EstadoSesionCaja;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

/** Una fila por sesión cerrada o pendiente de revisión. */
@Schema(description = "Sesión de caja con su apertura, cierre y totales por medio de pago")
public record CierreCajaFilaDTO(
        @Schema(description = "Id de la sesión", example = "12")
        long id_sesion,

        @Schema(description = "Fecha de apertura; define el período del reporte")
        LocalDate fechaApertura,

        @Schema(description = "Hora de apertura")
        LocalTime horaApertura,

        @Schema(description = "Fecha de cierre", nullable = true)
        LocalDate fechaCierre,

        @Schema(description = "Hora de cierre", nullable = true)
        LocalTime horaCierre,

        @Schema(description = "Nombre de quien abrió la sesión", example = "Ana Pérez")
        String abrio,

        @Schema(description = "Nombre de quien cerró la sesión", example = "Luis Gómez")
        String cerro,

        @Schema(description = "Monto inicial en efectivo", example = "100.00")
        BigDecimal montoInicial,

        @Schema(description = "CERRADA o PENDIENTE_REVISION")
        EstadoSesionCaja estado,

        @Schema(description = "True cuando la sesión está pendiente de revisión")
        boolean pendiente,

        @Schema(description = "Valores por medio de pago, con el medio como clave",
                example = "{\"EFECTIVO\": {...}}")
        Map<String, ValoresMedioPagoCierreDTO> valoresPorMedio
) {
}

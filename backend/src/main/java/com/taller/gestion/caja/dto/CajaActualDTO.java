package com.taller.gestion.caja.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Estado de la caja: sesión abierta (si existe), monto sugerido para "
        + "abrir y movimientos de la sesión abierta.")
public record CajaActualDTO(

        @Schema(description = "Sesión abierta, o null si no hay ninguna", nullable = true)
        SesionCajaResponseDTO sesion,
        @Schema(description = "Monto inicial sugerido para una nueva sesión")
        BigDecimal montoInicialSugerido,
        @Schema(description = "Movimientos de la sesión abierta")
        List<MovimientoCajaResponseDTO> movimientos
) {
}

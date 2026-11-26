package com.taller.gestion.caja.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Detalle de una sesión de caja con sus movimientos")
public record SesionCajaDetalleDTO(

        @Schema(description = "Datos y totales de la sesión") SesionCajaResponseDTO sesion,
        @Schema(description = "Movimientos de la sesión, del más nuevo al más viejo")
        List<MovimientoCajaResponseDTO> movimientos
) {
}

package com.taller.gestion.caja.dto;

import com.taller.gestion.caja.entity.ConceptoMovimientoCaja;
import com.taller.gestion.caja.entity.MedioPago;
import com.taller.gestion.caja.entity.TipoMovimientoCaja;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "Movimiento de caja registrado")
public record MovimientoCajaResponseDTO(

        @Schema(description = "Id del movimiento") Long id_movimiento_caja,
        @Schema(description = "Id de la sesión") Long id_sesion,
        @Schema(description = "Id del usuario que registró el movimiento") Long id_usuario,
        @Schema(description = "Nombre del usuario que registró el movimiento") String usuario,
        @Schema(description = "Id de la venta asociada, si corresponde", nullable = true)
        Long id_venta,
        @Schema(description = "Id del costo asociado, si corresponde", nullable = true)
        Long id_costo,
        @Schema(description = "INGRESO o EGRESO") TipoMovimientoCaja tipo,
        @Schema(description = "VENTA, RETIRO, GASTO o ANULACION") ConceptoMovimientoCaja concepto,
        @Schema(description = "Monto del movimiento") BigDecimal monto,
        @Schema(description = "EFECTIVO o TRANSFERENCIA") MedioPago medioPago,
        @Schema(description = "Fecha del movimiento") LocalDate fecha,
        @Schema(description = "Hora del movimiento") LocalTime hora,
        @Schema(description = "Observación libre", nullable = true) String observaciones
) {
}

package com.taller.gestion.venta.dto;

import com.taller.gestion.venta.entity.EstadoVenta;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "Venta confirmada o anulada con sus renglones")
public record VentaResponseDTO(

        @Schema(description = "Id de la venta")
        Long id_venta,

        @Schema(description = "Id del cliente")
        Long id_cliente,

        @Schema(description = "Nombre del cliente")
        String cliente,

        @Schema(description = "Id del usuario que cargó la venta")
        Long id_vendedor,

        @Schema(description = "Fecha de la venta")
        LocalDate fecha,

        @Schema(description = "Hora de la venta")
        LocalTime hora,

        @Schema(description = "Total calculado desde los renglones")
        BigDecimal total,

        @Schema(description = "Estado de la venta", allowableValues = {"CONFIRMADA", "ANULADA"})
        EstadoVenta estado,

        @Schema(description = "Vencimiento del saldo con seña, si corresponde", nullable = true)
        LocalDate fechaVencimiento,

        @Schema(description = "Renglones de la venta")
        List<DetalleVentaResponseDTO> detalles
) {
}

package com.taller.gestion.costos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Costo devuelto por la API. Los usuarios se exponen como id, nunca como entidad.")
public record CostoResponseDTO(
        @Schema(description = "Id del costo", example = "10") Long id_costo,
        @Schema(description = "Id del tipo de costo", example = "1") Long id_tipo_costo,
        @Schema(description = "Nombre del tipo de costo", example = "Alquiler") String nombreTipoCosto,
        @Schema(description = "Descripción del gasto", example = "Alquiler del local") String descripcion,
        @Schema(description = "Monto del costo", example = "150.00") BigDecimal monto,
        @Schema(description = "Fecha de pago", example = "2026-11-19") LocalDate fechaPago,
        @Schema(description = "Mes del período", example = "11") Integer periodoMes,
        @Schema(description = "Año del período", example = "2026") Integer periodoAnio,
        @Schema(description = "Id del usuario que cargó el costo", example = "7") Long id_usuario,
        @Schema(description = "Id del ingreso de mercadería, si el costo fue generado por uno") Long id_ingreso_mercaderia,
        @Schema(description = "Indica si el costo se generó desde un ingreso de mercadería", example = "false")
        boolean generadoPorIngreso,
        @Schema(description = "Indica si el costo está anulado", example = "false") boolean anulado,
        @Schema(description = "Motivo de la anulación, si la hubo") String motivoAnulacion,
        @Schema(description = "Id del usuario que anuló el costo, si se anuló", example = "7") Long id_usuario_anulacion,
        @Schema(description = "Fecha y hora de la anulación") LocalDateTime fechaAnulacion,
        @Schema(description = "Observaciones") String observaciones
) {}

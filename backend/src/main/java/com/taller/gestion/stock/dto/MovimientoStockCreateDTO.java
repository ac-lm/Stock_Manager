package com.taller.gestion.stock.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.taller.gestion.stock.entity.MotivoMovimientoStock;
import com.taller.gestion.stock.entity.TipoMovimientoStock;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Ajuste manual de stock. Debe venir exactamente un id: {@code idProducto} o {@code idInsumo}.
 * El usuario siempre se toma de la sesión, nunca del cuerpo.
 */
@Schema(description = "Entrada o salida manual de stock")
public record MovimientoStockCreateDTO(

        @Schema(description = "Id del producto; exclusivo con idInsumo", nullable = true,
                example = "1")
        Long idProducto,

        @Schema(description = "Id del insumo; exclusivo con idProducto", nullable = true,
                example = "2")
        Long idInsumo,

        @Schema(description = "ENTRADA suma al saldo; SALIDA lo descuenta",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "ENTRADA")
        @NotNull(message = "El tipo de movimiento es obligatorio")
        TipoMovimientoStock tipo,

        @Schema(description = "COMPRA, AJUSTE, MERMA o DEVOLUCION. INICIAL se genera al dar "
                + "de alta en el catálogo", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "AJUSTE")
        @NotNull(message = "El motivo es obligatorio")
        MotivoMovimientoStock motivo,

        @Schema(description = "Cantidad positiva con hasta 3 decimales",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "10.500")
        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor a cero")
        @Digits(integer = 9, fraction = 3, message = "La cantidad admite hasta 3 decimales")
        BigDecimal cantidad,

        @Schema(description = "Observación libre", nullable = true,
                example = "Conteo físico de fin de mes")
        @Size(max = 150, message = "La observación admite hasta 150 caracteres")
        String observaciones,

        @JsonIgnore
        @Schema(hidden = true)
        Long idUsuario
) {
}

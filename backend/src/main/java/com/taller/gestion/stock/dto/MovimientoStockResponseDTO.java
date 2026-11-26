package com.taller.gestion.stock.dto;

import com.taller.gestion.stock.entity.MotivoMovimientoStock;
import com.taller.gestion.stock.entity.TipoMovimientoStock;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Movimiento de stock registrado")
public record MovimientoStockResponseDTO(

        @Schema(description = "Id del movimiento") Long id_movimiento_stock,
        @Schema(description = "Id del producto, si el movimiento es de producto", nullable = true)
        Long id_producto,
        @Schema(description = "Nombre del producto, si corresponde", nullable = true)
        String nombreProducto,
        @Schema(description = "Id del insumo, si el movimiento es de insumo", nullable = true)
        Long id_insumo,
        @Schema(description = "Nombre del insumo, si corresponde", nullable = true)
        String nombreInsumo,
        @Schema(description = "ENTRADA o SALIDA") TipoMovimientoStock tipo,
        @Schema(description = "Motivo del movimiento") MotivoMovimientoStock motivo,
        @Schema(description = "Cantidad movida") BigDecimal cantidad,
        @Schema(description = "Id del usuario que registró el movimiento") Long id_usuario,
        @Schema(description = "Nombre del usuario que registró el movimiento") String usuario,
        @Schema(description = "Fecha y hora del movimiento") LocalDateTime fecha_hora,
        @Schema(description = "Observación libre", nullable = true) String observaciones,
        @Schema(description = "Id de la venta asociada, si corresponde", nullable = true)
        Long id_venta
) {
}

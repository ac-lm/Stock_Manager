package com.taller.gestion.venta.dto;

import com.taller.gestion.caja.dto.CobroVentaDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Alta de una venta. Se confirma en el mismo momento; el vendedor se toma "
        + "de la sesión y la fecha y hora del servidor. Requiere caja abierta y pagos que "
        + "sumen el total.")
public record VentaCreateDTO(

        @Schema(description = "Id del cliente. Si se omite, se usa el genérico 'Consumidor final'",
                example = "1", nullable = true)
        Long id_cliente,

        @Schema(description = "Vencimiento del saldo cuando hay seña. Opcional",
                example = "2026-12-15", nullable = true)
        LocalDate fechaVencimiento,

        @Schema(description = "Productos de la venta. Al menos uno")
        @NotEmpty(message = "La venta debe tener al menos un producto")
        List<@Valid DetalleVentaCreateDTO> detalles,

        @Schema(description = "Pagos de la venta. Al menos uno; la suma debe igualar el total "
                + "calculado por el servidor")
        @NotEmpty(message = "La venta debe tener al menos un pago")
        List<@Valid CobroVentaDTO> pagos
) {
}

package com.taller.gestion.venta.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Alta de una venta. Se confirma en el mismo momento; el vendedor se toma "
        + "de la sesión y la fecha y hora del servidor.")
public record VentaCreateDTO(

        @Schema(description = "Id del cliente. Si se omite, se usa el genérico 'Consumidor final'",
                example = "1", nullable = true)
        Long id_cliente,

        @Schema(description = "Vencimiento del saldo cuando hay seña. Opcional",
                example = "2026-12-15", nullable = true)
        LocalDate fechaVencimiento,

        @Schema(description = "Productos de la venta. Al menos uno")
        @NotEmpty(message = "La venta debe tener al menos un producto")
        List<@Valid DetalleVentaCreateDTO> detalles
) {
}

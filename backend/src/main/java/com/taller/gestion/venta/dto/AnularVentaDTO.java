package com.taller.gestion.venta.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos de la anulación de una venta")
public record AnularVentaDTO(

        @Schema(description = "Motivo de la anulación. Se guarda en las observaciones de los "
                + "contramovimientos de caja y de stock", example = "Producto defectuoso",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El motivo de la anulación es obligatorio")
        @Size(max = 150, message = "El motivo admite hasta 150 caracteres")
        String motivo
) {
}

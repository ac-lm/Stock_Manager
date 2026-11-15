package com.taller.gestion.clientes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Datos para cambiar el estado de un cliente")
public record ClienteEstadoDTO(
        @Schema(description = "true reactiva al cliente (vuelve a aparecer al vender); "
                + "false lo desactiva (baja lógica: conserva su historial y deja de "
                + "aparecer en las ventas nuevas)",
                example = "false",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El estado es obligatorio")
        Boolean activo
) {}
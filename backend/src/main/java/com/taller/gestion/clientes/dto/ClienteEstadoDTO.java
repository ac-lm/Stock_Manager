package com.taller.gestion.clientes.dto;

import jakarta.validation.constraints.NotNull;

public record ClienteEstadoDTO(
        @NotNull(message = "El estado es obligatorio")
        Boolean activo
) {}
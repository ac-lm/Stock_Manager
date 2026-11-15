package com.taller.gestion.incidencias.dto;

import com.taller.gestion.common.util.TextoUtil;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record IncidenciaCreateDTO(
        @NotNull(message = "El tipo de incidencia es obligatorio")
        Long id_tipo_incidencia,
        Long id_insumo,
        Long id_producto,
        @NotNull(message = "La cantidad afectada es obligatoria")
        @DecimalMin(value = "0.001", message = "La cantidad afectada debe ser mayor a cero")
        @Digits(integer = 9, fraction = 3, message = "La cantidad afectada admite hasta 3 decimales")
        BigDecimal cantidadAfectada,
        @Size(max = 100, message = "La descripción admite hasta 100 caracteres")
        String descripcion,
        @Size(max = 100, message = "Las observaciones admiten hasta 100 caracteres")
        String observaciones,
        Long id_usuario
) {
    public IncidenciaCreateDTO {
        descripcion = TextoUtil.limpiar(descripcion);
        observaciones = TextoUtil.limpiar(observaciones);
    }
}

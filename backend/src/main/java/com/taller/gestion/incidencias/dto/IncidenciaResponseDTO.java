package com.taller.gestion.incidencias.dto;

import com.taller.gestion.incidencias.entity.EstadoIncidencia;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record IncidenciaResponseDTO(
        Long id_incidencia,
        Long id_tipo_incidencia,
        String tipoIncidencia,
        Long id_insumo,
        String insumo,
        Long id_producto,
        String producto,
        String descripcion,
        BigDecimal cantidadAfectada,
        EstadoIncidencia estado,
        String observaciones,
        Long id_usuario,
        LocalDateTime fecha,
        Long id_usuario_resolucion,
        LocalDateTime fechaResolucion,
        String resolucionAdoptada
) {
}

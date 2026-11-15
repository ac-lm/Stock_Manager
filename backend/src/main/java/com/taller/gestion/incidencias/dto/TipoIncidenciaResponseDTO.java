package com.taller.gestion.incidencias.dto;

public record TipoIncidenciaResponseDTO(
        Long id_tipo_incidencia,
        String nombre,
        boolean activo
) {
}

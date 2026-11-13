package com.taller.gestion.incidencias.dto;

import lombok.Data;

@Data
public class IncidenciaResponseDTO {
    private Long id_incidencia;
    private String tipoIncidencia;
    private String descripcion;
    private String estado;
    private Long id_insumo;
}
package com.taller.gestion.incidencias.dto;

import lombok.Data;

@Data
public class IncidenciaResponseDTO {
    private Integer idIncidencia;
    private String tipoIncidencia;
    private String descripcion;
    private String estado;
    private Integer idInsumo;
}
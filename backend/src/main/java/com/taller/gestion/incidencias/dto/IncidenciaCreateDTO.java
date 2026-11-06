package com.taller.gestion.incidencias.dto;

import lombok.Data;

@Data
public class IncidenciaCreateDTO {
    private Integer idInsumo; // Opcional, dependiendo de la incidencia
    private Integer idProducto; // Opcional, dependiendo de la incidencia
    private String tipoIncidencia;
    private String descripcion;
    private Integer cantidadAfectada;
    private String observaciones;
}
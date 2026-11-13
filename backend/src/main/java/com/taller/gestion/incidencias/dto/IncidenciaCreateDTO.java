package com.taller.gestion.incidencias.dto;

import lombok.Data;

@Data
public class IncidenciaCreateDTO {
    private Long id_insumo; // Opcional, dependiendo de la incidencia
    private Long id_producto; // Opcional, dependiendo de la incidencia
    private String tipoIncidencia;
    private String descripcion;
    private Integer cantidadAfectada;
    private String observaciones;
}
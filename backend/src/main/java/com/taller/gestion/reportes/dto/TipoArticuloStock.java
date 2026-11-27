package com.taller.gestion.reportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Tipos de artículo que puede mostrar el reporte de stock. */
@Schema(description = "Tipo de artículo del reporte de stock")
public enum TipoArticuloStock {
    PRODUCTO,
    INSUMO
}

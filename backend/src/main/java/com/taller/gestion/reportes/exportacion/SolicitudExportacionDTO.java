package com.taller.gestion.reportes.exportacion;

import java.util.Map;

/**
 * Pedido genérico de exportación. Los filtros viajan como parámetros para que cada
 * reporte sepa interpretar los suyos sin cambiar este contrato.
 */
public record SolicitudExportacionDTO(
        String reporte,
        FormatoExportacion formato,
        Map<String, String> parametros) {

    public SolicitudExportacionDTO {
        parametros = Map.copyOf(parametros);
    }
}

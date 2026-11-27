package com.taller.gestion.reportes.exportacion;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.service.ReporteComparacionEstacionalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Conexión entre la exportación genérica y el reporte de REP-06. */
@Component
@RequiredArgsConstructor
public class ProveedorReporteComparacionEstacional implements ProveedorReporte {

    public static final String ID = "evolucion-ventas";

    private final ReporteComparacionEstacionalService reporteComparacionEstacionalService;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public ReporteExportable obtener(Map<String, String> parametros) {
        String anios = ParametrosReporte.texto(parametros, "anios", "");
        String categoria = ParametrosReporte.texto(parametros, "categoria", "");
        return reporteComparacionEstacionalService.comparacionEstacional(
                anios, idProducto(parametros), categoria);
    }

    private Long idProducto(Map<String, String> parametros) {
        String valor = ParametrosReporte.texto(parametros, "id_producto", "");
        if (valor.isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(valor);
        } catch (NumberFormatException ex) {
            throw new DatosInvalidosException("El parámetro 'id_producto' tiene un formato inválido");
        }
    }
}

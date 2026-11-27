package com.taller.gestion.reportes.exportacion;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.service.ReporteCostosPorTipoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Conexión entre la exportación genérica y el reporte de REP-10. */
@Component
@RequiredArgsConstructor
public class ProveedorReporteCostosPorTipo implements ProveedorReporte {

    public static final String ID = "costos-por-tipo";

    private final ReporteCostosPorTipoService reporteCostosPorTipoService;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public ReporteExportable obtener(Map<String, String> parametros) {
        return reporteCostosPorTipoService.costosPorTipo(
                ParametrosReporte.mes(parametros, "desde"),
                ParametrosReporte.mes(parametros, "hasta"),
                id(parametros, "id_tipo_costo"));
    }

    private Long id(Map<String, String> parametros, String nombre) {
        String valor = parametros.get(nombre);
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(valor.trim());
        } catch (NumberFormatException ex) {
            throw new DatosInvalidosException(
                    "El parámetro '" + nombre + "' tiene un formato inválido");
        }
    }
}

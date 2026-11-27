package com.taller.gestion.reportes.exportacion;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.service.ReporteStockActualService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Conexión entre la exportación genérica y el reporte de REP-08. */
@Component
@RequiredArgsConstructor
public class ProveedorReporteStockActual implements ProveedorReporte {

    public static final String ID = "stock-actual";

    private final ReporteStockActualService reporteStockActualService;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public ReporteExportable obtener(Map<String, String> parametros) {
        return reporteStockActualService.stockActual(
                ParametrosReporte.texto(parametros, "tipo", ""),
                ParametrosReporte.texto(parametros, "categoria", ""),
                booleano(parametros.get("solo_bajo_minimo")));
    }

    private boolean booleano(String valor) {
        if (valor == null || valor.isBlank()) {
            return false;
        }
        if ("true".equalsIgnoreCase(valor) || "1".equals(valor)) {
            return true;
        }
        if ("false".equalsIgnoreCase(valor) || "0".equals(valor)) {
            return false;
        }
        throw new DatosInvalidosException(
                "El parámetro 'solo_bajo_minimo' tiene un formato inválido");
    }
}

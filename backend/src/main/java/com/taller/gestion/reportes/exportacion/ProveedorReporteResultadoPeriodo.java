package com.taller.gestion.reportes.exportacion;

import com.taller.gestion.reportes.service.ReporteResultadoPeriodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Conexión entre la exportación genérica y el reporte de REP‑11. */
@Component
@RequiredArgsConstructor
public class ProveedorReporteResultadoPeriodo implements ProveedorReporte {

    public static final String ID = "resultado-periodo";

    private final ReporteResultadoPeriodoService reporteResultadoPeriodoService;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public ReporteExportable obtener(Map<String, String> parametros) {
        return reporteResultadoPeriodoService.resultadoPeriodo(
                ParametrosReporte.mes(parametros, "desde"),
                ParametrosReporte.mes(parametros, "hasta"));
    }
}

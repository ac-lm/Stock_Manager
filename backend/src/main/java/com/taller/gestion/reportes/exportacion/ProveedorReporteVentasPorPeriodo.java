package com.taller.gestion.reportes.exportacion;

import com.taller.gestion.reportes.service.ReporteVentasService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

/** Conexión entre la exportación genérica y el reporte de REP-01. */
@Component
@RequiredArgsConstructor
public class ProveedorReporteVentasPorPeriodo implements ProveedorReporte {

    public static final String ID = "ventas-por-periodo";

    private final ReporteVentasService reporteVentasService;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public ReporteExportable obtener(Map<String, String> parametros) {
        LocalDate desde = ParametrosReporte.fecha(parametros, "desde");
        LocalDate hasta = ParametrosReporte.fecha(parametros, "hasta");
        String agrupacion = ParametrosReporte.texto(parametros, "agrupacion", "dia");
        return reporteVentasService.ventasPorPeriodo(desde, hasta, agrupacion);
    }
}

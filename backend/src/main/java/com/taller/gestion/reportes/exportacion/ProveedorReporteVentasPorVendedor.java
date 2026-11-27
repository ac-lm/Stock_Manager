package com.taller.gestion.reportes.exportacion;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.service.ReporteVentasPorVendedorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

/** Conexión entre la exportación genérica y el reporte de REP-05. */
@Component
@RequiredArgsConstructor
public class ProveedorReporteVentasPorVendedor implements ProveedorReporte {

    public static final String ID = "ventas-por-vendedor";

    private final ReporteVentasPorVendedorService reporteVentasPorVendedorService;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public ReporteExportable obtener(Map<String, String> parametros) {
        LocalDate desde = ParametrosReporte.fecha(parametros, "desde");
        LocalDate hasta = ParametrosReporte.fecha(parametros, "hasta");

        return reporteVentasPorVendedorService.ventasPorVendedor(
                desde, hasta, idVendedor(parametros));
    }

    private Long idVendedor(Map<String, String> parametros) {
        String valor = ParametrosReporte.texto(parametros, "id_vendedor", "");
        if (valor.isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(valor);
        } catch (NumberFormatException ex) {
            throw new DatosInvalidosException("El parámetro 'id_vendedor' tiene un formato inválido");
        }
    }
}

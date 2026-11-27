package com.taller.gestion.reportes.exportacion;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.service.ReporteVentasPorClienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

/** Conexión entre la exportación genérica y el reporte de REP-04. */
@Component
@RequiredArgsConstructor
public class ProveedorReporteVentasPorCliente implements ProveedorReporte {

    public static final String ID = "ventas-por-cliente";

    private final ReporteVentasPorClienteService reporteVentasPorClienteService;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public ReporteExportable obtener(Map<String, String> parametros) {
        LocalDate desde = ParametrosReporte.fecha(parametros, "desde");
        LocalDate hasta = ParametrosReporte.fecha(parametros, "hasta");
        String tipoCliente = ParametrosReporte.texto(parametros, "tipo_cliente", "");

        return reporteVentasPorClienteService.ventasPorCliente(
                desde, hasta, idCliente(parametros), tipoCliente);
    }

    private Long idCliente(Map<String, String> parametros) {
        String valor = ParametrosReporte.texto(parametros, "id_cliente", "");
        if (valor.isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(valor);
        } catch (NumberFormatException ex) {
            throw new DatosInvalidosException("El parámetro 'id_cliente' tiene un formato inválido");
        }
    }
}

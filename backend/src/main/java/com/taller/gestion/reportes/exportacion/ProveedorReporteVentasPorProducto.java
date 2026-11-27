package com.taller.gestion.reportes.exportacion;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.service.ReporteVentasPorProductoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

/** Conexión entre la exportación genérica y el reporte de REP-03. */
@Component
@RequiredArgsConstructor
public class ProveedorReporteVentasPorProducto implements ProveedorReporte {

    public static final String ID = "ventas-por-producto";

    private final ReporteVentasPorProductoService reporteVentasPorProductoService;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public ReporteExportable obtener(Map<String, String> parametros) {
        LocalDate desde = ParametrosReporte.fecha(parametros, "desde");
        LocalDate hasta = ParametrosReporte.fecha(parametros, "hasta");
        String categoria = ParametrosReporte.texto(parametros, "categoria", "");

        return reporteVentasPorProductoService.ventasPorProducto(
                desde, hasta, idProducto(parametros), categoria, top(parametros));
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

    private Integer top(Map<String, String> parametros) {
        String valor = ParametrosReporte.texto(parametros, "top", "");
        if (valor.isEmpty()) {
            return ReporteVentasPorProductoService.TOP_DEFECTO;
        }
        try {
            return Integer.valueOf(valor);
        } catch (NumberFormatException ex) {
            throw new DatosInvalidosException("El parámetro 'top' debe ser un entero positivo");
        }
    }
}

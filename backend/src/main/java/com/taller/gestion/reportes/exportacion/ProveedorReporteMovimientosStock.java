package com.taller.gestion.reportes.exportacion;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.service.ReporteMovimientosStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Conexión entre la exportación genérica y el reporte de REP-09. */
@Component
@RequiredArgsConstructor
public class ProveedorReporteMovimientosStock implements ProveedorReporte {

    public static final String ID = "movimientos-stock";

    private final ReporteMovimientosStockService reporteMovimientosStockService;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public ReporteExportable obtener(Map<String, String> parametros) {
        return reporteMovimientosStockService.movimientosStock(
                ParametrosReporte.fecha(parametros, "desde"),
                ParametrosReporte.fecha(parametros, "hasta"),
                ParametrosReporte.texto(parametros, "tipo_articulo", ""),
                id(parametros, "id_articulo"),
                ParametrosReporte.texto(parametros, "tipo_movimiento", ""),
                id(parametros, "id_proveedor"));
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

package com.taller.gestion.reportes.exportacion;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.service.ReporteIncidenciasDescartesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

/** Conexión entre la exportación genérica y el reporte de REP-13. */
@Component
@RequiredArgsConstructor
public class ProveedorReporteIncidenciasDescartes implements ProveedorReporte {

    public static final String ID = "incidencias-descartes";

    private final ReporteIncidenciasDescartesService reporteIncidenciasDescartesService;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public ReporteExportable obtener(Map<String, String> parametros) {
        return reporteIncidenciasDescartesService.incidenciasDescartes(
                fecha(parametros, "desde"),
                fecha(parametros, "hasta"),
                id(opcional(parametros, "id_tipo_incidencia"), "id_tipo_incidencia"),
                opcional(parametros, "estado"),
                opcional(parametros, "tipo_articulo"),
                id(opcional(parametros, "id_articulo"), "id_articulo"));
    }

    private LocalDate fecha(Map<String, String> parametros, String nombre) {
        String valor = parametros.get(nombre);
        if (valor == null || valor.isBlank()) {
            throw new DatosInvalidosException("El parámetro '" + nombre + "' es obligatorio");
        }
        try {
            return LocalDate.parse(valor.trim());
        } catch (RuntimeException ex) {
            throw new DatosInvalidosException(
                    "El parámetro '" + nombre + "' debe tener el formato AAAA-MM-DD");
        }
    }

    private String opcional(Map<String, String> parametros, String nombre) {
        String valor = parametros.get(nombre);
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private Long id(String valor, String nombre) {
        if (valor == null) {
            return null;
        }
        try {
            return Long.valueOf(valor);
        } catch (NumberFormatException ex) {
            throw new DatosInvalidosException("El parámetro '" + nombre + "' debe ser un número");
        }
    }
}

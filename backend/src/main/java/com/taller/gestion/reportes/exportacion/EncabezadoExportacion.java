package com.taller.gestion.reportes.exportacion;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** Texto común que llevan los tres formatos: título, filtros y fecha de emisión. */
@Component
@RequiredArgsConstructor
public class EncabezadoExportacion {

    private final FormateadorFechaEmision formateadorFechaEmision;

    public List<String> lineas(ReporteExportable reporte) {
        return List.of(
                reporte.titulo(),
                "Desde: " + reporte.desde(),
                "Hasta: " + reporte.hasta(),
                reporte.descripcionFiltros(),
                "Emitido: " + formateadorFechaEmision.formatear());
    }
}

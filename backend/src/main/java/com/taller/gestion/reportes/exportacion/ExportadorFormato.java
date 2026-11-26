package com.taller.gestion.reportes.exportacion;

/** Implementación de un formato concreto. Se elige por {@link #formato()}. */
public interface ExportadorFormato {

    FormatoExportacion formato();

    ArchivoExportadoDTO exportar(ReporteExportable reporte);
}

package com.taller.gestion.reportes.exportacion;

import java.time.LocalDate;
import java.util.List;

/**
 * Contrato que cumple cualquier DTO de reporte que quiera exportarse.
 * El generador de archivos no necesita conocer el reporte concreto.
 */
public interface ReporteExportable {

    /** Identificador usado en el nombre del archivo, por ejemplo "ventas-por-periodo". */
    String id();

    String titulo();

    /** Filtros aplicados, en texto, para que el archivo explique qué datos contiene. */
    String descripcionFiltros();

    LocalDate desde();

    LocalDate hasta();

    List<ColumnaExportable> columnas();

    /** Cada fila tiene un valor por columna, en el mismo orden. */
    List<List<Object>> filasExportables();

    /** Última fila del archivo, con un valor por columna. */
    List<Object> totalesExportables();
}

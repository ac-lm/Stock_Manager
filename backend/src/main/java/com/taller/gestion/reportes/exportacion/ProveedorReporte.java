package com.taller.gestion.reportes.exportacion;

import java.util.Map;

/** Cada reporte registra un proveedor: convierte sus filtros en el DTO que ya ve la pantalla. */
public interface ProveedorReporte {

    String id();

    ReporteExportable obtener(Map<String, String> parametros);
}

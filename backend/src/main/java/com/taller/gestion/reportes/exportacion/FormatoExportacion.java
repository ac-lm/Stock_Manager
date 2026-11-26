package com.taller.gestion.reportes.exportacion;

import com.taller.gestion.common.exception.DatosInvalidosException;

/** Formatos soportados por la exportación común de reportes. */
public enum FormatoExportacion {
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    CSV("csv", "text/csv;charset=UTF-8"),
    PDF("pdf", "application/pdf");

    private final String extension;
    private final String contentType;

    FormatoExportacion(String extension, String contentType) {
        this.extension = extension;
        this.contentType = contentType;
    }

    public String extension() {
        return extension;
    }

    public String contentType() {
        return contentType;
    }

    public static FormatoExportacion desde(String valor) {
        if (valor != null) {
            for (FormatoExportacion formato : values()) {
                if (formato.extension.equalsIgnoreCase(valor.trim())) {
                    return formato;
                }
            }
        }
        throw new DatosInvalidosException("El formato debe ser xlsx, csv o pdf");
    }
}

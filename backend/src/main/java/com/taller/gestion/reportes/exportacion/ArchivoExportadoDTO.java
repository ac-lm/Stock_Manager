package com.taller.gestion.reportes.exportacion;

/** Resultado binario de una exportación, listo para devolver al navegador. */
public record ArchivoExportadoDTO(byte[] contenido, String contentType, String nombreArchivo) {

    public ArchivoExportadoDTO {
        contenido = contenido.clone();
    }

    @Override
    public byte[] contenido() {
        return contenido.clone();
    }
}

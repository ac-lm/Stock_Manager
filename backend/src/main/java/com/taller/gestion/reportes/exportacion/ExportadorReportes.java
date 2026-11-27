package com.taller.gestion.reportes.exportacion;

import com.taller.gestion.common.exception.DatosInvalidosException;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Fachada de exportación: busca el reporte por id, usa exactamente el DTO de la pantalla
 * y delega el archivo al generador del formato pedido.
 */
@Service
public class ExportadorReportes {

    private final RegistroProveedoresReporte registro;
    private final Map<FormatoExportacion, ExportadorFormato> exportadores;

    public ExportadorReportes(RegistroProveedoresReporte registro,
                              List<ExportadorFormato> exportadores) {
        this.registro = registro;
        this.exportadores = new EnumMap<>(FormatoExportacion.class);
        for (ExportadorFormato exportador : exportadores) {
            this.exportadores.put(exportador.formato(), exportador);
        }
    }

    public ArchivoExportadoDTO exportar(SolicitudExportacionDTO solicitud) {
        ProveedorReporte proveedor = registro.obtener(solicitud.reporte());
        ReporteExportable reporte = proveedor.obtener(solicitud.parametros());

        ExportadorFormato exportador = exportadores.get(solicitud.formato());
        if (exportador == null) {
            throw new DatosInvalidosException(
                    "No hay un generador para el formato " + solicitud.formato());
        }

        ArchivoExportadoDTO archivo = exportador.exportar(reporte);
        String rango = reporte.nombreRango() == null
                ? reporte.desde() + "_" + reporte.hasta()
                : reporte.nombreRango();
        String nombre = reporte.id() + "_" + rango
                + "." + solicitud.formato().extension();
        return new ArchivoExportadoDTO(
                archivo.contenido(), archivo.contentType(), nombre);
    }
}

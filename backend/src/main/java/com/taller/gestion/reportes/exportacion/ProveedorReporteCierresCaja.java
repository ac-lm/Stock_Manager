package com.taller.gestion.reportes.exportacion;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.service.ReporteCierresCajaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

/** Conexión entre la exportación genérica y el reporte de REP-07. */
@Component
@RequiredArgsConstructor
public class ProveedorReporteCierresCaja implements ProveedorReporte {

    public static final String ID = "cierres-caja";

    private final ReporteCierresCajaService reporteCierresCajaService;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public ReporteExportable obtener(Map<String, String> parametros) {
        LocalDate desde = ParametrosReporte.fecha(parametros, "desde");
        LocalDate hasta = ParametrosReporte.fecha(parametros, "hasta");

        return reporteCierresCajaService.cierresCaja(
                desde, hasta, idUsuario(parametros),
                ParametrosReporte.texto(parametros, "estado", "TODOS"),
                ParametrosReporte.texto(parametros, "medio_pago", "TODOS"));
    }

    private Long idUsuario(Map<String, String> parametros) {
        String valor = ParametrosReporte.texto(parametros, "id_usuario_cierre", "");
        if (valor.isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(valor);
        } catch (NumberFormatException ex) {
            throw new DatosInvalidosException(
                    "El parámetro 'id_usuario_cierre' tiene un formato inválido");
        }
    }
}

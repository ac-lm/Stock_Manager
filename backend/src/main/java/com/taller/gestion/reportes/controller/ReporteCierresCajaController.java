package com.taller.gestion.reportes.controller;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.ReporteCierresCajaDTO;
import com.taller.gestion.reportes.service.ReporteCierresCajaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteCierresCajaController implements ReporteCierresCajaApi {

    private final ReporteCierresCajaService reporteCierresCajaService;

    @GetMapping("/cierres-caja")
    public ReporteCierresCajaDTO cierresCaja(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(required = false) String id_usuario_cierre,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String medio_pago) {

        return reporteCierresCajaService.cierresCaja(
                aFecha(desde, "desde"), aFecha(hasta, "hasta"),
                aId(id_usuario_cierre, "id_usuario_cierre"), estado, medio_pago);
    }

    /** Un parámetro ausente o con formato inválido se responde como 400, no como 500. */
    private LocalDate aFecha(String valor, String nombre) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(valor);
        } catch (RuntimeException ex) {
            throw new DatosInvalidosException(
                    "El parámetro '" + nombre + "' tiene un formato inválido");
        }
    }

    private Long aId(String valor, String nombre) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(valor.trim());
        } catch (NumberFormatException ex) {
            throw new DatosInvalidosException("El parámetro '" + nombre + "' tiene un formato inválido");
        }
    }
}

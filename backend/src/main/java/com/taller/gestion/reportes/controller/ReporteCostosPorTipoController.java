package com.taller.gestion.reportes.controller;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.ReporteCostosPorTipoDTO;
import com.taller.gestion.reportes.exportacion.ParametrosReporte;
import com.taller.gestion.reportes.service.ReporteCostosPorTipoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteCostosPorTipoController implements ReporteCostosPorTipoApi {

    private final ReporteCostosPorTipoService reporteCostosPorTipoService;

    @GetMapping("/costos-por-tipo")
    public ReporteCostosPorTipoDTO costosPorTipo(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(required = false) String id_tipo_costo) {

        return reporteCostosPorTipoService.costosPorTipo(
                ParametrosReporte.mes(desde, "desde"),
                ParametrosReporte.mes(hasta, "hasta"),
                aId(id_tipo_costo, "id_tipo_costo"));
    }

    private Long aId(String valor, String nombre) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(valor.trim());
        } catch (NumberFormatException ex) {
            throw new DatosInvalidosException(
                    "El parámetro '" + nombre + "' tiene un formato inválido");
        }
    }
}

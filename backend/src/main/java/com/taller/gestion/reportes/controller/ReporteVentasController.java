package com.taller.gestion.reportes.controller;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.ReporteVentasPorPeriodoDTO;
import com.taller.gestion.reportes.service.ReporteVentasService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteVentasController implements ReporteVentasApi {

    private final ReporteVentasService reporteVentasService;

    @GetMapping("/ventas-por-periodo")
    public ReporteVentasPorPeriodoDTO ventasPorPeriodo(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(required = false, defaultValue = "dia") String agrupacion) {

        return reporteVentasService.ventasPorPeriodo(
                aFecha(desde, "desde"), aFecha(hasta, "hasta"), agrupacion);
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
}

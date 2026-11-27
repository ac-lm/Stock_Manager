package com.taller.gestion.reportes.controller;

import com.taller.gestion.reportes.dto.ReporteResultadoPeriodoDTO;
import com.taller.gestion.reportes.exportacion.ParametrosReporte;
import com.taller.gestion.reportes.service.ReporteResultadoPeriodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteResultadoPeriodoController implements ReporteResultadoPeriodoApi {

    private final ReporteResultadoPeriodoService reporteResultadoPeriodoService;

    @GetMapping("/resultado-periodo")
    public ReporteResultadoPeriodoDTO resultadoPeriodo(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta) {

        return reporteResultadoPeriodoService.resultadoPeriodo(
                ParametrosReporte.mes(desde, "desde"),
                ParametrosReporte.mes(hasta, "hasta"));
    }
}

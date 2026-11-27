package com.taller.gestion.reportes.controller;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.ReporteStockActualDTO;
import com.taller.gestion.reportes.service.ReporteStockActualService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteStockActualController implements ReporteStockActualApi {

    private final ReporteStockActualService reporteStockActualService;

    @GetMapping("/stock-actual")
    public ReporteStockActualDTO stockActual(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String solo_bajo_minimo) {

        return reporteStockActualService.stockActual(
                tipo, categoria, aBooleano(solo_bajo_minimo));
    }

    private boolean aBooleano(String valor) {
        if (valor == null || valor.isBlank()) {
            return false;
        }
        if ("true".equalsIgnoreCase(valor) || "1".equals(valor)) {
            return true;
        }
        if ("false".equalsIgnoreCase(valor) || "0".equals(valor)) {
            return false;
        }
        throw new DatosInvalidosException(
                "El parámetro 'solo_bajo_minimo' tiene un formato inválido");
    }
}

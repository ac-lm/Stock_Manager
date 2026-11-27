package com.taller.gestion.reportes.controller;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.ReporteComparacionEstacionalDTO;
import com.taller.gestion.reportes.service.ReporteComparacionEstacionalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteComparacionEstacionalController implements ReporteComparacionEstacionalApi {

    private final ReporteComparacionEstacionalService reporteComparacionEstacionalService;

    @GetMapping("/evolucion-ventas")
    public ReporteComparacionEstacionalDTO comparacionEstacional(
            @RequestParam(required = false) String anios,
            @RequestParam(required = false) String id_producto,
            @RequestParam(required = false) String categoria) {

        return reporteComparacionEstacionalService.comparacionEstacional(
                anios, aIdProducto(id_producto), categoria);
    }

    /** Un filtro con formato inválido se responde como 400, no como 500. */
    private Long aIdProducto(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(valor.trim());
        } catch (NumberFormatException ex) {
            throw new DatosInvalidosException("El parámetro 'id_producto' tiene un formato inválido");
        }
    }
}

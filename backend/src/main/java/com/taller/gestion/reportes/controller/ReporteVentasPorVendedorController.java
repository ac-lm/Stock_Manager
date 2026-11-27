package com.taller.gestion.reportes.controller;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.ReporteVentasPorVendedorDTO;
import com.taller.gestion.reportes.service.ReporteVentasPorVendedorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteVentasPorVendedorController implements ReporteVentasPorVendedorApi {

    private final ReporteVentasPorVendedorService reporteVentasPorVendedorService;

    @GetMapping("/ventas-por-vendedor")
    public ReporteVentasPorVendedorDTO ventasPorVendedor(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(required = false) String id_vendedor) {

        return reporteVentasPorVendedorService.ventasPorVendedor(
                aFecha(desde, "desde"), aFecha(hasta, "hasta"), aIdVendedor(id_vendedor));
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

    private Long aIdVendedor(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(valor.trim());
        } catch (NumberFormatException ex) {
            throw new DatosInvalidosException("El parámetro 'id_vendedor' tiene un formato inválido");
        }
    }
}

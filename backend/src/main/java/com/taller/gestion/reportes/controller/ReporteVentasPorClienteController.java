package com.taller.gestion.reportes.controller;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.ReporteVentasPorClienteDTO;
import com.taller.gestion.reportes.service.ReporteVentasPorClienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteVentasPorClienteController implements ReporteVentasPorClienteApi {

    private final ReporteVentasPorClienteService reporteVentasPorClienteService;

    @GetMapping("/ventas-por-cliente")
    public ReporteVentasPorClienteDTO ventasPorCliente(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(required = false) String id_cliente,
            @RequestParam(required = false) String tipo_cliente) {

        return reporteVentasPorClienteService.ventasPorCliente(
                aFecha(desde, "desde"), aFecha(hasta, "hasta"),
                aIdCliente(id_cliente), tipo_cliente);
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

    private Long aIdCliente(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(valor.trim());
        } catch (NumberFormatException ex) {
            throw new DatosInvalidosException("El parámetro 'id_cliente' tiene un formato inválido");
        }
    }
}

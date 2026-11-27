package com.taller.gestion.reportes.controller;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.ReporteVentasPorProductoDTO;
import com.taller.gestion.reportes.service.ReporteVentasPorProductoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteVentasPorProductoController implements ReporteVentasPorProductoApi {

    private final ReporteVentasPorProductoService reporteVentasPorProductoService;

    @GetMapping("/ventas-por-producto")
    public ReporteVentasPorProductoDTO ventasPorProducto(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(required = false) String id_producto,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false, defaultValue = "10") String top) {

        return reporteVentasPorProductoService.ventasPorProducto(
                aFecha(desde, "desde"), aFecha(hasta, "hasta"),
                aIdProducto(id_producto), categoria, aTop(top));
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

    private Integer aTop(String valor) {
        if (valor == null || valor.isBlank()) {
            return ReporteVentasPorProductoService.TOP_DEFECTO;
        }
        try {
            return Integer.valueOf(valor.trim());
        } catch (NumberFormatException ex) {
            throw new DatosInvalidosException("El parámetro 'top' debe ser un entero positivo");
        }
    }
}

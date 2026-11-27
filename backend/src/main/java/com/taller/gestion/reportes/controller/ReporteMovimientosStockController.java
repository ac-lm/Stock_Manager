package com.taller.gestion.reportes.controller;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.ReporteMovimientosStockDTO;
import com.taller.gestion.reportes.service.ReporteMovimientosStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteMovimientosStockController implements ReporteMovimientosStockApi {

    private final ReporteMovimientosStockService reporteMovimientosStockService;

    @GetMapping("/movimientos-stock")
    public ReporteMovimientosStockDTO movimientosStock(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(required = false) String tipo_articulo,
            @RequestParam(required = false) String id_articulo,
            @RequestParam(required = false) String tipo_movimiento,
            @RequestParam(required = false) String id_proveedor) {

        return reporteMovimientosStockService.movimientosStock(
                aFecha(desde, "desde"), aFecha(hasta, "hasta"),
                tipo_articulo, aId(id_articulo, "id_articulo"),
                tipo_movimiento, aId(id_proveedor, "id_proveedor"));
    }

    /** Un parámetro ausente o con formato inválido se responde como 400, no como 500. */
    private LocalDate aFecha(String valor, String nombre) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(valor.trim());
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
            throw new DatosInvalidosException(
                    "El parámetro '" + nombre + "' tiene un formato inválido");
        }
    }
}

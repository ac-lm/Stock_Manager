package com.taller.gestion.reportes.controller;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.ReporteIncidenciasDescartesDTO;
import com.taller.gestion.reportes.service.ReporteIncidenciasDescartesService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteIncidenciasDescartesController implements ReporteIncidenciasDescartesApi {

    private final ReporteIncidenciasDescartesService reporteIncidenciasDescartesService;

    @GetMapping("/incidencias-descartes")
    public ReporteIncidenciasDescartesDTO incidenciasDescartes(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(name = "id_tipo_incidencia", required = false) Long idTipoIncidencia,
            @RequestParam(required = false) String estado,
            @RequestParam(name = "tipo_articulo", required = false) String tipoArticulo,
            @RequestParam(name = "id_articulo", required = false) Long idArticulo) {

        if (desde == null || desde.isBlank()) {
            throw new DatosInvalidosException("El parámetro 'desde' es obligatorio");
        }
        if (hasta == null || hasta.isBlank()) {
            throw new DatosInvalidosException("El parámetro 'hasta' es obligatorio");
        }
        LocalDate fechaDesde;
        LocalDate fechaHasta;
        try {
            fechaDesde = LocalDate.parse(desde.trim());
            fechaHasta = LocalDate.parse(hasta.trim());
        } catch (RuntimeException ex) {
            throw new DatosInvalidosException("Las fechas deben tener el formato AAAA-MM-DD");
        }
        return reporteIncidenciasDescartesService.incidenciasDescartes(
                fechaDesde, fechaHasta, idTipoIncidencia, estado, tipoArticulo, idArticulo);
    }
}

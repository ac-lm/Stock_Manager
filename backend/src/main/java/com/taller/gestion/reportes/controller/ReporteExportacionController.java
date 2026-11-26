package com.taller.gestion.reportes.controller;

import com.taller.gestion.reportes.exportacion.ArchivoExportadoDTO;
import com.taller.gestion.reportes.exportacion.ExportadorReportes;
import com.taller.gestion.reportes.exportacion.FormatoExportacion;
import com.taller.gestion.reportes.exportacion.SolicitudExportacionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteExportacionController implements ReporteExportacionApi {

    private final ExportadorReportes exportadorReportes;

    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(
            @RequestParam String reporte,
            @RequestParam String formato,
            @RequestParam Map<String, String> parametros) {

        ArchivoExportadoDTO archivo = exportadorReportes.exportar(new SolicitudExportacionDTO(
                reporte, FormatoExportacion.desde(formato), parametros));

        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.setContentType(MediaType.parseMediaType(archivo.contentType()));
        cabeceras.add(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + archivo.nombreArchivo() + "\"");
        cabeceras.setContentLength(archivo.contenido().length);
        return new ResponseEntity<>(archivo.contenido(), cabeceras, HttpStatus.OK);
    }
}

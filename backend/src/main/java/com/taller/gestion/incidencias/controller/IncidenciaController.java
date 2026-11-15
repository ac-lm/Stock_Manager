package com.taller.gestion.incidencias.controller;

import com.taller.gestion.incidencias.dto.IncidenciaCreateDTO;
import com.taller.gestion.incidencias.dto.IncidenciaResolucionDTO;
import com.taller.gestion.incidencias.dto.IncidenciaResponseDTO;
import com.taller.gestion.incidencias.dto.IncidenciaUpdateDTO;
import com.taller.gestion.incidencias.entity.EstadoIncidencia;
import com.taller.gestion.incidencias.service.IncidenciaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/incidencias")
@RequiredArgsConstructor
public class IncidenciaController {

    private final IncidenciaService incidenciaService;

    @GetMapping
    public Page<IncidenciaResponseDTO> listar(
            @RequestParam(required = false) EstadoIncidencia estado,
            @RequestParam(required = false) Long id_tipo_incidencia,
            @RequestParam(required = false) Long id_insumo,
            @RequestParam(required = false) Long id_producto,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @PageableDefault(size = 20, sort = "fecha", direction = Sort.Direction.DESC) Pageable pageable) {
        return incidenciaService.listarIncidencias(
                estado, id_tipo_incidencia, id_insumo, id_producto, desde, hasta, pageable);
    }

    @PostMapping
    public ResponseEntity<IncidenciaResponseDTO> crearIncidencia(
            @Valid @RequestBody IncidenciaCreateDTO dto) {
        IncidenciaResponseDTO nuevaIncidencia = incidenciaService.crearIncidencia(dto);
        return new ResponseEntity<>(nuevaIncidencia, HttpStatus.CREATED);
    }

    @GetMapping("/{id_incidencia}")
    public IncidenciaResponseDTO obtenerIncidencia(@PathVariable Long id_incidencia) {
        return incidenciaService.obtenerIncidencia(id_incidencia);
    }

    @PutMapping("/{id_incidencia}")
    public IncidenciaResponseDTO actualizarIncidencia(
            @PathVariable Long id_incidencia,
            @Valid @RequestBody IncidenciaUpdateDTO dto) {
        return incidenciaService.actualizarIncidencia(id_incidencia, dto);
    }

    @PutMapping("/{id_incidencia}/resolver")
    public IncidenciaResponseDTO resolverIncidencia(
            @PathVariable Long id_incidencia,
            @Valid @RequestBody IncidenciaResolucionDTO dto) {
        return incidenciaService.resolverIncidencia(id_incidencia, dto);
    }

    @DeleteMapping("/{id_incidencia}")
    public ResponseEntity<Void> eliminarIncidencia(@PathVariable Long id_incidencia) {
        incidenciaService.eliminarIncidencia(id_incidencia);
        return ResponseEntity.noContent().build();
    }
}

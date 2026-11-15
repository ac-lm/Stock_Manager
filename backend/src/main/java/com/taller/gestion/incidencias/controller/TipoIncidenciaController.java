package com.taller.gestion.incidencias.controller;

import com.taller.gestion.incidencias.dto.TipoIncidenciaCreateDTO;
import com.taller.gestion.incidencias.dto.TipoIncidenciaResponseDTO;
import com.taller.gestion.incidencias.dto.TipoIncidenciaUpdateDTO;
import com.taller.gestion.incidencias.service.TipoIncidenciaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidencias/tipos")
@RequiredArgsConstructor
public class TipoIncidenciaController {

    private final TipoIncidenciaService tipoIncidenciaService;

    @GetMapping
    public ResponseEntity<List<TipoIncidenciaResponseDTO>> listarTipos() {
        return ResponseEntity.ok(tipoIncidenciaService.listarTipos());
    }

    @PostMapping
    public ResponseEntity<TipoIncidenciaResponseDTO> crearTipo(
            @Valid @RequestBody TipoIncidenciaCreateDTO dto) {
        TipoIncidenciaResponseDTO nuevoTipo = tipoIncidenciaService.crearTipo(dto);
        return new ResponseEntity<>(nuevoTipo, HttpStatus.CREATED);
    }

    @PutMapping("/{id_tipo_incidencia}")
    public TipoIncidenciaResponseDTO actualizarTipo(
            @PathVariable Long id_tipo_incidencia,
            @Valid @RequestBody TipoIncidenciaUpdateDTO dto) {
        return tipoIncidenciaService.actualizarTipo(id_tipo_incidencia, dto);
    }

    @DeleteMapping("/{id_tipo_incidencia}")
    public ResponseEntity<Void> eliminarTipo(@PathVariable Long id_tipo_incidencia) {
        tipoIncidenciaService.eliminarTipo(id_tipo_incidencia);
        return ResponseEntity.noContent().build();
    }
}

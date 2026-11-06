package com.taller.gestion.incidencias.controller;

import com.taller.gestion.incidencias.dto.IncidenciaCreateDTO;
import com.taller.gestion.incidencias.dto.IncidenciaResponseDTO;
import com.taller.gestion.incidencias.entity.Incidencia;
import com.taller.gestion.incidencias.service.IncidenciaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/incidencias")
@RequiredArgsConstructor
public class IncidenciaController {

    private final IncidenciaService incidenciaService;

    @PostMapping
    public ResponseEntity<IncidenciaResponseDTO> crearIncidencia(@RequestBody IncidenciaCreateDTO dto) {
        Incidencia incidencia = incidenciaService.crearIncidencia(dto);

        IncidenciaResponseDTO response = new IncidenciaResponseDTO();
        response.setIdIncidencia(incidencia.getIdIncidencia());
        response.setTipoIncidencia(incidencia.getTipoIncidencia());
        response.setDescripcion(incidencia.getDescripcion());
        response.setEstado(incidencia.getEstado());
        if (incidencia.getInsumo() != null) {
            response.setIdInsumo(incidencia.getInsumo().getIdInsumo());
        }

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
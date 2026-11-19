package com.taller.gestion.costos.controller;

import com.taller.gestion.costos.dto.*;
import com.taller.gestion.costos.service.CostoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/costos")
@RequiredArgsConstructor
public class CostoController implements CostoApi {
    private final CostoService costoService;

    @GetMapping
    public Page<CostoResponseDTO> listar(
            @RequestParam(required = false) Long id_tipo_costo,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer anio,
            @RequestParam(defaultValue = "false") boolean incluirAnulados,
            @PageableDefault(size = 20, sort = "fechaPago", direction = Sort.Direction.DESC) Pageable pageable) {
        return costoService.buscar(id_tipo_costo, mes, anio, incluirAnulados, pageable);
    }

    // Total de los costos que cumplen el filtro (sin contar los anulados)
    @GetMapping("/total")
    public BigDecimal total(@RequestParam(required = false) Long id_tipo_costo,
                            @RequestParam(required = false) Integer mes,
                            @RequestParam(required = false) Integer anio) {
        return costoService.total(id_tipo_costo, mes, anio);
    }

    @GetMapping("/{id_costo}")
    public CostoResponseDTO obtener(@PathVariable Long id_costo) {
        return costoService.findById(id_costo);
    }

    @PostMapping
    public ResponseEntity<CostoResponseDTO> crear(@Valid @RequestBody CostoCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(costoService.crear(dto));
    }

    @PutMapping("/{id_costo}")
    public CostoResponseDTO actualizar(@PathVariable Long id_costo,
                                       @Valid @RequestBody CostoUpdateDTO dto) {
        return costoService.actualizar(id_costo, dto);
    }

    @PatchMapping("/{id_costo}/anulacion")
    public CostoResponseDTO anular(@PathVariable Long id_costo,
                                   @Valid @RequestBody CostoAnularDTO dto) {
        return costoService.anular(id_costo, dto);
    }
}

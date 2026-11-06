package com.taller.gestion.catalogo.controller;

import com.taller.gestion.catalogo.dto.InsumoCreateDTO;
import com.taller.gestion.catalogo.entity.Insumo;
import com.taller.gestion.catalogo.service.InsumoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalogo/insumos")
@RequiredArgsConstructor
public class InsumoController {

    private final InsumoService insumoService;

    @PostMapping
    public ResponseEntity<Insumo> crearInsumo(@RequestBody InsumoCreateDTO dto) {
        Insumo nuevoInsumo = insumoService.crearInsumo(dto);
        return new ResponseEntity<>(nuevoInsumo, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Insumo> obtenerInsumo(@PathVariable Integer id) {
        Insumo insumo = insumoService.obtenerPorId(id);
        return ResponseEntity.ok(insumo);
    }
}
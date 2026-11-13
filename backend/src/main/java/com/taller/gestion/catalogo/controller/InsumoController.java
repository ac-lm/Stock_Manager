package com.taller.gestion.catalogo.controller;

import com.taller.gestion.catalogo.dto.InsumoCreateDTO;
import com.taller.gestion.catalogo.dto.InsumoResponseDTO;
import com.taller.gestion.catalogo.dto.InsumoUpdateDTO;
import com.taller.gestion.catalogo.service.InsumoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalogo/insumos")
@RequiredArgsConstructor
public class InsumoController {

    private final InsumoService insumoService;

    @GetMapping
    public ResponseEntity<List<InsumoResponseDTO>> listarInsumos() {
        return ResponseEntity.ok(insumoService.listarInsumos());
    }

    @GetMapping("/{id_insumo}")
    public ResponseEntity<InsumoResponseDTO> obtenerInsumo(@PathVariable Long id_insumo) {
        return ResponseEntity.ok(insumoService.obtenerInsumo(id_insumo));
    }

    @PostMapping
    public ResponseEntity<InsumoResponseDTO> crearInsumo(@RequestBody InsumoCreateDTO dto) {
        InsumoResponseDTO nuevoInsumo = insumoService.crearInsumo(dto);
        return new ResponseEntity<>(nuevoInsumo, HttpStatus.CREATED);
    }

    @PutMapping("/{id_insumo}")
    public ResponseEntity<InsumoResponseDTO> actualizarInsumo(
            @PathVariable Long id_insumo,
            @RequestBody InsumoUpdateDTO dto) {
        return ResponseEntity.ok(insumoService.actualizarInsumo(id_insumo, dto));
    }

    @DeleteMapping("/{id_insumo}")
    public ResponseEntity<Void> eliminarInsumo(@PathVariable Long id_insumo) {
        insumoService.eliminarInsumo(id_insumo);
        return ResponseEntity.noContent().build();
    }
}

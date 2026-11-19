package com.taller.gestion.costos.controller;

import com.taller.gestion.costos.dto.*;
import com.taller.gestion.costos.service.TipoCostoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tipos-costo")
@RequiredArgsConstructor
public class TipoCostoController implements TipoCostoApi {
    private final TipoCostoService tipoCostoService;

    @GetMapping
    public List<TipoCostoResponseDTO> listar(@RequestParam(defaultValue = "false") boolean incluirInactivos) {
        return tipoCostoService.listar(incluirInactivos);
    }

    @PostMapping
    public ResponseEntity<TipoCostoResponseDTO> crear(@Valid @RequestBody TipoCostoCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tipoCostoService.crear(dto));
    }

    @PutMapping("/{id_tipo_costo}")
    public TipoCostoResponseDTO actualizar(@PathVariable Long id_tipo_costo,
                                           @Valid @RequestBody TipoCostoUpdateDTO dto) {
        return tipoCostoService.actualizar(id_tipo_costo, dto);
    }

    @PatchMapping("/{id_tipo_costo}/estado")
    public TipoCostoResponseDTO cambiarEstado(@PathVariable Long id_tipo_costo,
                                              @Valid @RequestBody EstadoDTO dto) {
        return tipoCostoService.cambiarEstado(id_tipo_costo, dto.activo());
    }
}

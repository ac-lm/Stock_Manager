package com.taller.gestion.proveedores.controller;

import com.taller.gestion.proveedores.dto.ProveedorCreateDTO;
import com.taller.gestion.proveedores.dto.ProveedorResponseDTO;
import com.taller.gestion.proveedores.dto.ProveedorUpdateDTO;
import com.taller.gestion.proveedores.service.ProveedorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/proveedores")
@RequiredArgsConstructor
public class ProveedorController {

    private final ProveedorService proveedorService;

    @GetMapping
    public ResponseEntity<List<ProveedorResponseDTO>> listarProveedores() {
        return ResponseEntity.ok(proveedorService.listarProveedores());
    }

    @GetMapping("/{id_proveedor}")
    public ResponseEntity<ProveedorResponseDTO> obtenerProveedor(@PathVariable Long id_proveedor) {
        return ResponseEntity.ok(proveedorService.obtenerProveedor(id_proveedor));
    }

    @PostMapping
    public ResponseEntity<ProveedorResponseDTO> crearProveedor(@RequestBody ProveedorCreateDTO dto) {
        ProveedorResponseDTO nuevoProveedor = proveedorService.crearProveedor(dto);
        return new ResponseEntity<>(nuevoProveedor, HttpStatus.CREATED);
    }

    @PutMapping("/{id_proveedor}")
    public ResponseEntity<ProveedorResponseDTO> actualizarProveedor(
            @PathVariable Long id_proveedor,
            @RequestBody ProveedorUpdateDTO dto) {
        return ResponseEntity.ok(proveedorService.actualizarProveedor(id_proveedor, dto));
    }

    @DeleteMapping("/{id_proveedor}")
    public ResponseEntity<Void> eliminarProveedor(@PathVariable Long id_proveedor) {
        proveedorService.eliminarProveedor(id_proveedor);
        return ResponseEntity.noContent().build();
    }
}

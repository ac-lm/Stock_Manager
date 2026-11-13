package com.taller.gestion.proveedores.controller;

import com.taller.gestion.proveedores.dto.ProveedorInsumoCreateDTO;
import com.taller.gestion.proveedores.dto.ProveedorInsumoResponseDTO;
import com.taller.gestion.proveedores.service.ProveedorInsumoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/proveedores/{id_proveedor}/insumos")
@RequiredArgsConstructor
public class ProveedorInsumoController {

    private final ProveedorInsumoService proveedorInsumoService;

    @GetMapping
    public ResponseEntity<List<ProveedorInsumoResponseDTO>> listarInsumosPorProveedor(
            @PathVariable Long id_proveedor) {
        return ResponseEntity.ok(proveedorInsumoService.listarInsumosPorProveedor(id_proveedor));
    }

    @PostMapping
    public ResponseEntity<ProveedorInsumoResponseDTO> asignarInsumo(
            @PathVariable Long id_proveedor,
            @RequestBody ProveedorInsumoCreateDTO dto) {
        ProveedorInsumoResponseDTO asignacion = proveedorInsumoService.asignarInsumo(id_proveedor, dto);
        return new ResponseEntity<>(asignacion, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id_proveedor_insumo}")
    public ResponseEntity<Void> eliminarAsignacion(@PathVariable Long id_proveedor_insumo) {
        proveedorInsumoService.eliminarAsignacion(id_proveedor_insumo);
        return ResponseEntity.noContent().build();
    }
}

package com.taller.gestion.catalogo.controller;

import com.taller.gestion.catalogo.dto.ProductoInsumoCreateDTO;
import com.taller.gestion.catalogo.dto.ProductoInsumoResponseDTO;
import com.taller.gestion.catalogo.dto.ProductoInsumoUpdateDTO;
import com.taller.gestion.catalogo.service.ProductoInsumoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalogo/productos/{id_producto}/insumos")
@RequiredArgsConstructor
public class ProductoInsumoController {

    private final ProductoInsumoService productoInsumoService;

    @GetMapping
    public ResponseEntity<List<ProductoInsumoResponseDTO>> listarInsumosPorProducto(
            @PathVariable Long id_producto) {
        return ResponseEntity.ok(productoInsumoService.listarInsumosPorProducto(id_producto));
    }

    @PostMapping
    public ResponseEntity<ProductoInsumoResponseDTO> asignarInsumo(
            @PathVariable Long id_producto,
            @RequestBody ProductoInsumoCreateDTO dto) {
        ProductoInsumoResponseDTO asignacion = productoInsumoService.asignarInsumo(id_producto, dto);
        return new ResponseEntity<>(asignacion, HttpStatus.CREATED);
    }

    @PutMapping("/{id_producto_insumo}")
    public ResponseEntity<ProductoInsumoResponseDTO> actualizarAsignacion(
            @PathVariable Long id_producto_insumo,
            @RequestBody ProductoInsumoUpdateDTO dto) {
        return ResponseEntity.ok(productoInsumoService.actualizarAsignacion(id_producto_insumo, dto));
    }

    @DeleteMapping("/{id_producto_insumo}")
    public ResponseEntity<Void> eliminarAsignacion(@PathVariable Long id_producto_insumo) {
        productoInsumoService.eliminarAsignacion(id_producto_insumo);
        return ResponseEntity.noContent().build();
    }
}

package com.taller.gestion.catalogo.controller;

import com.taller.gestion.catalogo.dto.ProductoCreateDTO;
import com.taller.gestion.catalogo.dto.ProductoResponseDTO;
import com.taller.gestion.catalogo.dto.ProductoUpdateDTO;
import com.taller.gestion.catalogo.service.ProductoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalogo/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    @GetMapping
    public ResponseEntity<List<ProductoResponseDTO>> listarProductos() {
        return ResponseEntity.ok(productoService.listarProductos());
    }

    @GetMapping("/{id_producto}")
    public ResponseEntity<ProductoResponseDTO> obtenerProducto(@PathVariable Long id_producto) {
        return ResponseEntity.ok(productoService.obtenerProducto(id_producto));
    }

    @PostMapping
    public ResponseEntity<ProductoResponseDTO> crearProducto(@RequestBody ProductoCreateDTO dto) {
        ProductoResponseDTO nuevoProducto = productoService.crearProducto(dto);
        return new ResponseEntity<>(nuevoProducto, HttpStatus.CREATED);
    }

    @PutMapping("/{id_producto}")
    public ResponseEntity<ProductoResponseDTO> actualizarProducto(
            @PathVariable Long id_producto,
            @RequestBody ProductoUpdateDTO dto) {
        return ResponseEntity.ok(productoService.actualizarProducto(id_producto, dto));
    }

    @DeleteMapping("/{id_producto}")
    public ResponseEntity<Void> eliminarProducto(@PathVariable Long id_producto) {
        productoService.eliminarProducto(id_producto);
        return ResponseEntity.noContent().build();
    }
}

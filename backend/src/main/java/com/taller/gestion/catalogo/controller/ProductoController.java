package com.taller.gestion.catalogo.controller;

import com.taller.gestion.catalogo.dto.ProductoCreateDTO;
import com.taller.gestion.catalogo.entity.Producto;
import com.taller.gestion.catalogo.service.ProductoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalogo/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    @PostMapping
    public ResponseEntity<Producto> crearProducto(@RequestBody ProductoCreateDTO dto) {
        Producto nuevoProducto = productoService.crearProducto(dto);
        return new ResponseEntity<>(nuevoProducto, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Producto> obtenerProducto(@PathVariable Integer id) {
        Producto producto = productoService.obtenerPorId(id);
        return ResponseEntity.ok(producto);
    }
}
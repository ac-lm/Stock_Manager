package com.taller.gestion.catalogo.service;

import com.taller.gestion.catalogo.dto.ProductoCreateDTO;
import com.taller.gestion.catalogo.entity.Producto;
import com.taller.gestion.catalogo.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;

    public Producto obtenerPorId(Integer id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));
    }
    public Producto crearProducto(ProductoCreateDTO dto) {
        Producto producto = new Producto();
        producto.setNombre(dto.nombre());
        producto.setPrecio(dto.precio());
        producto.setStockActual(dto.stockActual() != null ? dto.stockActual() : BigDecimal.ZERO);
        producto.setCategoria(dto.categoria());

        return productoRepository.save(producto);
    }
}
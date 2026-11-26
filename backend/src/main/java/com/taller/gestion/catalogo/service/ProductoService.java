package com.taller.gestion.catalogo.service;

import com.taller.gestion.catalogo.dto.ProductoCreateDTO;
import com.taller.gestion.catalogo.dto.ProductoResponseDTO;
import com.taller.gestion.catalogo.dto.ProductoUpdateDTO;
import com.taller.gestion.catalogo.entity.Producto;
import com.taller.gestion.catalogo.repository.ProductoRepository;
import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.common.exception.RecursoNoEncontradoException;
import com.taller.gestion.stock.entity.MotivoMovimientoStock;
import com.taller.gestion.stock.entity.TipoMovimientoStock;
import com.taller.gestion.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;

    // StockService usa ProductoRepository, no este servicio: no hay ciclo de dependencias.
    private final StockService stockService;

    @Transactional(readOnly = true)
    public List<ProductoResponseDTO> listarProductos() {
        return productoRepository.findAllByOrderByNombreAsc()
                .stream()
                .map(this::mapearAResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponseDTO obtenerProducto(Long id_producto) {
        return mapearAResponse(obtenerPorId(id_producto));
    }

    /**
     * Devuelve la entidad para uso interno del módulo y de otros módulos
     * que necesiten asociar un producto (siempre a través de este servicio).
     */
    @Transactional(readOnly = true)
    public Producto obtenerPorId(Long id_producto) {
        return productoRepository.findById(id_producto)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Producto no encontrado con ID: " + id_producto));
    }

    @Transactional
    public ProductoResponseDTO crearProducto(ProductoCreateDTO dto) {
        validarNombreYCategoria(dto.nombre(), dto.categoria());

        BigDecimal stockInicial = dto.stockActual() != null ? dto.stockActual() : BigDecimal.ZERO;
        if (stockInicial.compareTo(BigDecimal.ZERO) < 0) {
            throw new DatosInvalidosException("El stock inicial no puede ser negativo");
        }

        Producto producto = new Producto();
        producto.setNombre(dto.nombre());
        producto.setPrecio(dto.precio() != null ? dto.precio() : BigDecimal.ZERO);
        producto.setStockActual(BigDecimal.ZERO);
        producto.setCategoria(dto.categoria());

        Producto guardado = productoRepository.save(producto);
        if (stockInicial.compareTo(BigDecimal.ZERO) > 0) {
            stockService.registrarMovimiento(
                    guardado.getId_producto(), null, TipoMovimientoStock.ENTRADA,
                    MotivoMovimientoStock.INICIAL, stockInicial, "Stock inicial del alta");
        }
        return mapearAResponse(guardado);
    }

    @Transactional
    public ProductoResponseDTO actualizarProducto(Long id_producto, ProductoUpdateDTO dto) {
        Producto producto = obtenerPorId(id_producto);

        if (dto.nombre() != null && dto.nombre().isBlank()) {
            throw new DatosInvalidosException("El nombre del producto no puede estar vacío");
        }
        if (dto.categoria() != null && dto.categoria().isBlank()) {
            throw new DatosInvalidosException("La categoría del producto no puede estar vacía");
        }

        if (dto.nombre() != null) {
            producto.setNombre(dto.nombre());
        }
        if (dto.precio() != null) {
            producto.setPrecio(dto.precio());
        }
        if (dto.categoria() != null) {
            producto.setCategoria(dto.categoria());
        }

        return mapearAResponse(productoRepository.save(producto));
    }

    @Transactional
    public void eliminarProducto(Long id_producto) {
        productoRepository.delete(obtenerPorId(id_producto));
    }

    private void validarNombreYCategoria(String nombre, String categoria) {
        if (nombre == null || nombre.isBlank()) {
            throw new DatosInvalidosException("El nombre del producto es obligatorio");
        }
        if (categoria == null || categoria.isBlank()) {
            throw new DatosInvalidosException("La categoría del producto es obligatoria");
        }
    }

    private ProductoResponseDTO mapearAResponse(Producto producto) {
        return new ProductoResponseDTO(
                producto.getId_producto(),
                producto.getNombre(),
                producto.getPrecio(),
                producto.getStockActual(),
                producto.getCategoria()
        );
    }
}

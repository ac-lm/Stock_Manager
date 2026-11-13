package com.taller.gestion.catalogo.service;

import com.taller.gestion.catalogo.dto.ProductoInsumoCreateDTO;
import com.taller.gestion.catalogo.dto.ProductoInsumoResponseDTO;
import com.taller.gestion.catalogo.dto.ProductoInsumoUpdateDTO;
import com.taller.gestion.catalogo.entity.Insumo;
import com.taller.gestion.catalogo.entity.Producto;
import com.taller.gestion.catalogo.entity.ProductoInsumo;
import com.taller.gestion.catalogo.exception.DatosInvalidosException;
import com.taller.gestion.catalogo.exception.ProductoInsumoDuplicadoException;
import com.taller.gestion.catalogo.exception.ProductoInsumoNotFoundException;
import com.taller.gestion.catalogo.repository.ProductoInsumoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoInsumoService {

    private final ProductoInsumoRepository productoInsumoRepository;

    // Comunicación dentro del módulo a través de servicios, nunca de repositorios.
    private final ProductoService productoService;
    private final InsumoService insumoService;

    @Transactional(readOnly = true)
    public List<ProductoInsumoResponseDTO> listarInsumosPorProducto(Long id_producto) {
        productoService.obtenerPorId(id_producto);

        return productoInsumoRepository.findPorProducto(id_producto)
                .stream()
                .map(this::mapearAResponse)
                .toList();
    }

    @Transactional
    public ProductoInsumoResponseDTO asignarInsumo(Long id_producto, ProductoInsumoCreateDTO dto) {
        if (dto.id_insumo() == null) {
            throw new DatosInvalidosException("El ID del insumo es obligatorio");
        }
        if (dto.cantNecesaria() == null) {
            throw new DatosInvalidosException("La cantidad necesaria es obligatoria");
        }
        if (dto.unidadMedida() == null || dto.unidadMedida().isBlank()) {
            throw new DatosInvalidosException("La unidad de medida es obligatoria");
        }

        Producto producto = productoService.obtenerPorId(id_producto);
        Insumo insumo = insumoService.obtenerPorId(dto.id_insumo());

        if (productoInsumoRepository.buscarPorProductoEInsumo(id_producto, dto.id_insumo()).isPresent()) {
            throw new ProductoInsumoDuplicadoException(
                    "El producto ya tiene asignado el insumo con ID: " + dto.id_insumo());
        }

        ProductoInsumo asignacion = new ProductoInsumo();
        asignacion.setProducto(producto);
        asignacion.setInsumo(insumo);
        asignacion.setCantNecesaria(dto.cantNecesaria());
        asignacion.setUnidadMedida(dto.unidadMedida());

        return mapearAResponse(productoInsumoRepository.save(asignacion));
    }

    @Transactional
    public ProductoInsumoResponseDTO actualizarAsignacion(Long id_producto_insumo, ProductoInsumoUpdateDTO dto) {
        ProductoInsumo asignacion = obtenerPorId(id_producto_insumo);

        if (dto.unidadMedida() != null && dto.unidadMedida().isBlank()) {
            throw new DatosInvalidosException("La unidad de medida no puede estar vacía");
        }

        if (dto.cantNecesaria() != null) {
            asignacion.setCantNecesaria(dto.cantNecesaria());
        }
        if (dto.unidadMedida() != null) {
            asignacion.setUnidadMedida(dto.unidadMedida());
        }

        return mapearAResponse(productoInsumoRepository.save(asignacion));
    }

    @Transactional
    public void eliminarAsignacion(Long id_producto_insumo) {
        productoInsumoRepository.delete(obtenerPorId(id_producto_insumo));
    }

    @Transactional(readOnly = true)
    public ProductoInsumo obtenerPorId(Long id_producto_insumo) {
        return productoInsumoRepository.findById(id_producto_insumo)
                .orElseThrow(() -> new ProductoInsumoNotFoundException(
                        "Asignación producto-insumo no encontrada con ID: " + id_producto_insumo));
    }

    private ProductoInsumoResponseDTO mapearAResponse(ProductoInsumo asignacion) {
        return new ProductoInsumoResponseDTO(
                asignacion.getId_producto_insumo(),
                asignacion.getProducto().getId_producto(),
                asignacion.getProducto().getNombre(),
                asignacion.getInsumo().getId_insumo(),
                asignacion.getInsumo().getNombre(),
                asignacion.getCantNecesaria(),
                asignacion.getUnidadMedida()
        );
    }
}

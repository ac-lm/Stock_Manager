package com.taller.gestion.proveedores.service;

import com.taller.gestion.proveedores.dto.ProveedorCreateDTO;
import com.taller.gestion.proveedores.dto.ProveedorResponseDTO;
import com.taller.gestion.proveedores.dto.ProveedorUpdateDTO;
import com.taller.gestion.proveedores.entity.Proveedor;
import com.taller.gestion.proveedores.exception.ProveedorDuplicadoException;
import com.taller.gestion.proveedores.exception.ProveedorNotFoundException;
import com.taller.gestion.proveedores.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;

    @Transactional(readOnly = true)
    public List<ProveedorResponseDTO> listarProveedores() {
        return proveedorRepository.findAllByOrderByNombreAsc()
                .stream()
                .map(this::mapearAResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProveedorResponseDTO obtenerProveedor(Long id_proveedor) {
        return mapearAResponse(obtenerPorId(id_proveedor));
    }

    /**
     * Devuelve la entidad para uso interno del módulo y de otros módulos
     * que necesiten asociar un proveedor (siempre a través de este servicio).
     */
    @Transactional(readOnly = true)
    public Proveedor obtenerPorId(Long id_proveedor) {
        return proveedorRepository.findById(id_proveedor)
                .orElseThrow(() -> new ProveedorNotFoundException(
                        "Proveedor no encontrado con ID: " + id_proveedor));
    }

    @Transactional
    public ProveedorResponseDTO crearProveedor(ProveedorCreateDTO dto) {
        if (dto.cuit() != null && proveedorRepository.existsByCuit(dto.cuit())) {
            throw new ProveedorDuplicadoException("Ya existe un proveedor con el CUIT: " + dto.cuit());
        }

        Proveedor proveedor = new Proveedor();
        proveedor.setNombre(dto.nombre());
        proveedor.setCuit(dto.cuit());
        proveedor.setTelefono(dto.telefono());
        proveedor.setEmail(dto.email());
        proveedor.setTipo_producto(dto.tipo_producto());
        proveedor.setObservaciones(dto.observaciones());

        return mapearAResponse(proveedorRepository.save(proveedor));
    }

    @Transactional
    public ProveedorResponseDTO actualizarProveedor(Long id_proveedor, ProveedorUpdateDTO dto) {
        Proveedor proveedor = obtenerPorId(id_proveedor);

        if (dto.cuit() != null
                && !dto.cuit().equals(proveedor.getCuit())
                && proveedorRepository.existsByCuit(dto.cuit())) {
            throw new ProveedorDuplicadoException("Ya existe un proveedor con el CUIT: " + dto.cuit());
        }

        if (dto.nombre() != null) {
            proveedor.setNombre(dto.nombre());
        }
        if (dto.cuit() != null) {
            proveedor.setCuit(dto.cuit());
        }
        if (dto.telefono() != null) {
            proveedor.setTelefono(dto.telefono());
        }
        if (dto.email() != null) {
            proveedor.setEmail(dto.email());
        }
        if (dto.tipo_producto() != null) {
            proveedor.setTipo_producto(dto.tipo_producto());
        }
        if (dto.observaciones() != null) {
            proveedor.setObservaciones(dto.observaciones());
        }

        return mapearAResponse(proveedorRepository.save(proveedor));
    }

    /**
     * Elimina el proveedor y, por cascada, sus relaciones con insumos.
     */
    @Transactional
    public void eliminarProveedor(Long id_proveedor) {
        Proveedor proveedor = obtenerPorId(id_proveedor);
        proveedorRepository.delete(proveedor);
    }

    private ProveedorResponseDTO mapearAResponse(Proveedor proveedor) {
        return new ProveedorResponseDTO(
                proveedor.getId_proveedor(),
                proveedor.getNombre(),
                proveedor.getCuit(),
                proveedor.getTelefono(),
                proveedor.getEmail(),
                proveedor.getTipo_producto(),
                proveedor.getObservaciones()
        );
    }
}

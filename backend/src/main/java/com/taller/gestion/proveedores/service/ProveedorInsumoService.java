package com.taller.gestion.proveedores.service;

import com.taller.gestion.catalogo.entity.Insumo;
import com.taller.gestion.catalogo.service.InsumoService;
import com.taller.gestion.proveedores.dto.ProveedorInsumoCreateDTO;
import com.taller.gestion.proveedores.dto.ProveedorInsumoResponseDTO;
import com.taller.gestion.proveedores.entity.Proveedor;
import com.taller.gestion.proveedores.entity.ProveedorInsumo;
import com.taller.gestion.proveedores.exception.ProveedorDuplicadoException;
import com.taller.gestion.proveedores.exception.ProveedorInsumoNotFoundException;
import com.taller.gestion.proveedores.repository.ProveedorInsumoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProveedorInsumoService {

    private final ProveedorInsumoRepository proveedorInsumoRepository;

    // Comunicación entre módulos a través de servicios, nunca de repositorios.
    private final ProveedorService proveedorService;
    private final InsumoService insumoService;

    @Transactional(readOnly = true)
    public List<ProveedorInsumoResponseDTO> listarInsumosPorProveedor(Long id_proveedor) {
        proveedorService.obtenerPorId(id_proveedor);

        return proveedorInsumoRepository.findPorProveedor(id_proveedor)
                .stream()
                .map(this::mapearAResponse)
                .toList();
    }

    @Transactional
    public ProveedorInsumoResponseDTO asignarInsumo(Long id_proveedor, ProveedorInsumoCreateDTO dto) {
        if (dto.id_insumo() == null) {
            throw new IllegalArgumentException("El ID del insumo es obligatorio");
        }

        Proveedor proveedor = proveedorService.obtenerPorId(id_proveedor);
        Insumo insumo = insumoService.obtenerPorId(dto.id_insumo());

        if (proveedorInsumoRepository.buscarPorProveedorEInsumo(id_proveedor, dto.id_insumo()).isPresent()) {
            throw new ProveedorDuplicadoException(
                    "El proveedor ya tiene asignado el insumo con ID: " + dto.id_insumo());
        }

        ProveedorInsumo asignacion = new ProveedorInsumo();
        asignacion.setProveedor(proveedor);
        asignacion.setInsumo(insumo);

        return mapearAResponse(proveedorInsumoRepository.save(asignacion));
    }

    /**
     * Elimina la relación proveedor-insumo.
     */
    @Transactional
    public void eliminarAsignacion(Long id_proveedor_insumo) {
        ProveedorInsumo asignacion = obtenerPorId(id_proveedor_insumo);
        proveedorInsumoRepository.delete(asignacion);
    }

    @Transactional(readOnly = true)
    public ProveedorInsumo obtenerPorId(Long id_proveedor_insumo) {
        return proveedorInsumoRepository.findById(id_proveedor_insumo)
                .orElseThrow(() -> new ProveedorInsumoNotFoundException(
                        "Asignación proveedor-insumo no encontrada con ID: " + id_proveedor_insumo));
    }

    private ProveedorInsumoResponseDTO mapearAResponse(ProveedorInsumo asignacion) {
        return new ProveedorInsumoResponseDTO(
                asignacion.getId_proveedor_insumo(),
                asignacion.getProveedor().getId_proveedor(),
                asignacion.getProveedor().getNombre(),
                asignacion.getInsumo().getId_insumo(),
                asignacion.getInsumo().getNombre()
        );
    }
}

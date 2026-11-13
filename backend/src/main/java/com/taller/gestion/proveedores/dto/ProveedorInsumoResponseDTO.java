package com.taller.gestion.proveedores.dto;

public record ProveedorInsumoResponseDTO(
        Long id_proveedor_insumo,
        Long id_proveedor,
        String nombre_proveedor,
        Long id_insumo,
        String nombre_insumo
) {
}

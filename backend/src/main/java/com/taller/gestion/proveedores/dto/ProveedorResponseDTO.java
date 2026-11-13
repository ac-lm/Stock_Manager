package com.taller.gestion.proveedores.dto;

public record ProveedorResponseDTO(
        Long id_proveedor,
        String nombre,
        String cuit,
        String telefono,
        String email,
        String tipo_producto,
        String observaciones
) {
}

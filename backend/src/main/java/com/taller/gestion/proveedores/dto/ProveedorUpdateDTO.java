package com.taller.gestion.proveedores.dto;

public record ProveedorUpdateDTO(
        String nombre,
        String cuit,
        String telefono,
        String email,
        String tipo_producto,
        String observaciones
) {
}

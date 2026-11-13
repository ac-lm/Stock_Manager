package com.taller.gestion.catalogo.dto;

public record ProductoInsumoResponseDTO(
        Long id_producto_insumo,
        Long id_producto,
        String nombreProducto,
        Long id_insumo,
        String nombreInsumo,
        Float cantNecesaria,
        String unidadMedida
) {
}

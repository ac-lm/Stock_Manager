package com.taller.gestion.catalogo.dto;

public record ProductoInsumoCreateDTO(
        Long id_insumo,
        Float cantNecesaria,
        String unidadMedida
) {
}

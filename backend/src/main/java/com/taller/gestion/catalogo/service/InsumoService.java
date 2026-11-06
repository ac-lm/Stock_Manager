package com.taller.gestion.catalogo.service;

import com.taller.gestion.catalogo.dto.InsumoCreateDTO;
import com.taller.gestion.catalogo.entity.Insumo;
import com.taller.gestion.catalogo.repository.InsumoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InsumoService {

    private final InsumoRepository insumoRepository;

    public Insumo obtenerPorId(Integer id) {
        return insumoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Insumo no encontrado con ID: " + id));
    }
    public Insumo crearInsumo(InsumoCreateDTO dto) {
        Insumo insumo = new Insumo();
        insumo.setNombre(dto.nombre());
        insumo.setStockActual(dto.stockActual() != null ? dto.stockActual() : 0);
        insumo.setUnidadMedida(dto.unidadMedida());
        insumo.setStockMinimo(dto.stockMinimo() != null ? dto.stockMinimo() : 0);
        insumo.setCosto(dto.costo());

        return insumoRepository.save(insumo);
    }
}
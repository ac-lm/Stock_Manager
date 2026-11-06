package com.taller.gestion.catalogo.service;

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
}
package com.taller.gestion.incidencias.service;

import com.taller.gestion.catalogo.entity.Insumo;
import com.taller.gestion.catalogo.service.InsumoService;
import com.taller.gestion.incidencias.dto.IncidenciaCreateDTO;
import com.taller.gestion.incidencias.entity.Incidencia;
import com.taller.gestion.incidencias.repository.IncidenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IncidenciaService {

    private final IncidenciaRepository incidenciaRepository;
    private final InsumoService insumoService;
    // private final ProductoService productoService; // Cuando exista Producto

    @Transactional
    public Incidencia crearIncidencia(IncidenciaCreateDTO dto) {
        Incidencia nuevaIncidencia = new Incidencia();

        if (dto.getIdInsumo() != null) {
            Insumo insumo = insumoService.obtenerPorId(dto.getIdInsumo());
            nuevaIncidencia.setInsumo(insumo);
        }

        // if (dto.getIdProducto() != null) {
        //     Producto producto = productoService.obtenerPorId(dto.getIdProducto());
        //     nuevaIncidencia.setProducto(producto);
        // }

        nuevaIncidencia.setTipoIncidencia(dto.getTipoIncidencia());
        nuevaIncidencia.setDescripcion(dto.getDescripcion());
        nuevaIncidencia.setCantidadAfectada(dto.getCantidadAfectada());
        nuevaIncidencia.setObservaciones(dto.getObservaciones());
        nuevaIncidencia.setEstado("PENDIENTE");

        return incidenciaRepository.save(nuevaIncidencia);
    }
}
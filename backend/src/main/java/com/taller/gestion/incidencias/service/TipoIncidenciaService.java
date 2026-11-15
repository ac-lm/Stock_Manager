package com.taller.gestion.incidencias.service;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.common.exception.RecursoDuplicadoException;
import com.taller.gestion.common.exception.RecursoNoEncontradoException;
import com.taller.gestion.incidencias.dto.TipoIncidenciaCreateDTO;
import com.taller.gestion.incidencias.dto.TipoIncidenciaResponseDTO;
import com.taller.gestion.incidencias.dto.TipoIncidenciaUpdateDTO;
import com.taller.gestion.incidencias.entity.TipoIncidencia;
import com.taller.gestion.incidencias.repository.TipoIncidenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TipoIncidenciaService {

    private final TipoIncidenciaRepository tipoIncidenciaRepository;

    @Transactional(readOnly = true)
    public List<TipoIncidenciaResponseDTO> listarTipos() {
        return tipoIncidenciaRepository.findAllByOrderByNombreAsc()
                .stream()
                .map(this::mapearAResponse)
                .toList();
    }

    @Transactional
    public TipoIncidenciaResponseDTO crearTipo(TipoIncidenciaCreateDTO dto) {
        validarNombre(dto.nombre());

        if (tipoIncidenciaRepository.existeNombre(dto.nombre(), null)) {
            throw new RecursoDuplicadoException(
                    "Ya existe un tipo de incidencia con el nombre '" + dto.nombre() + "'");
        }

        TipoIncidencia tipo = new TipoIncidencia();
        tipo.setNombre(dto.nombre());
        tipo.setActivo(true);

        return mapearAResponse(tipoIncidenciaRepository.save(tipo));
    }

    @Transactional
    public TipoIncidenciaResponseDTO actualizarTipo(Long id_tipo_incidencia, TipoIncidenciaUpdateDTO dto) {
        TipoIncidencia tipo = obtenerPorId(id_tipo_incidencia);
        validarNombre(dto.nombre());

        if (tipoIncidenciaRepository.existeNombre(dto.nombre(), id_tipo_incidencia)) {
            throw new RecursoDuplicadoException(
                    "Ya existe un tipo de incidencia con el nombre '" + dto.nombre() + "'");
        }

        tipo.setNombre(dto.nombre());
        if (dto.activo() != null) {
            tipo.setActivo(dto.activo());
        }

        return mapearAResponse(tipoIncidenciaRepository.save(tipo));
    }

    /**
     * Baja lógica: el tipo queda inactivo para no romper las incidencias históricas
     * que lo referencian.
     */
    @Transactional
    public void eliminarTipo(Long id_tipo_incidencia) {
        TipoIncidencia tipo = obtenerPorId(id_tipo_incidencia);
        tipo.setActivo(false);
        tipoIncidenciaRepository.save(tipo);
    }

    @Transactional(readOnly = true)
    public TipoIncidencia obtenerPorId(Long id_tipo_incidencia) {
        return tipoIncidenciaRepository.findById(id_tipo_incidencia)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Tipo de incidencia no encontrado con ID: " + id_tipo_incidencia));
    }

    /**
     * Devuelve el tipo solo si está activo. Se usa al crear o modificar una incidencia.
     */
    @Transactional(readOnly = true)
    public TipoIncidencia obtenerActivoPorId(Long id_tipo_incidencia) {
        TipoIncidencia tipo = obtenerPorId(id_tipo_incidencia);
        if (!tipo.isActivo()) {
            throw new DatosInvalidosException(
                    "El tipo de incidencia '" + tipo.getNombre() + "' está inactivo");
        }
        return tipo;
    }

    private void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new DatosInvalidosException("El nombre del tipo de incidencia es obligatorio");
        }
    }

    private TipoIncidenciaResponseDTO mapearAResponse(TipoIncidencia tipo) {
        return new TipoIncidenciaResponseDTO(
                tipo.getId_tipo_incidencia(),
                tipo.getNombre(),
                tipo.isActivo()
        );
    }
}

package com.taller.gestion.costos.service;

import com.taller.gestion.common.exception.OperacionNoPermitidaException;
import com.taller.gestion.common.exception.RecursoDuplicadoException;
import com.taller.gestion.common.exception.RecursoNoEncontradoException;
import com.taller.gestion.costos.dto.TipoCostoCreateDTO;
import com.taller.gestion.costos.dto.TipoCostoResponseDTO;
import com.taller.gestion.costos.dto.TipoCostoUpdateDTO;
import com.taller.gestion.costos.entity.TipoCosto;
import com.taller.gestion.costos.repository.TipoCostoRepository;
import com.taller.gestion.util.TextoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TipoCostoService {
    private final TipoCostoRepository tipoCostoRepository;

    @Transactional(readOnly = true)
    public List<TipoCostoResponseDTO> listar(boolean incluirInactivos) {
        return tipoCostoRepository.listar(incluirInactivos).stream()
                .map(this::mapearAResponse)
                .toList();
    }

    @Transactional
    public TipoCostoResponseDTO crear(TipoCostoCreateDTO dto) {
        if (tipoCostoRepository.existsByNombreNormalizado(TextoUtil.normalizarParaBusqueda(dto.nombre()))) {
            throw new RecursoDuplicadoException("Ya existe un tipo de costo con ese nombre");
        }
        TipoCosto tipo = new TipoCosto();
        tipo.setNombre(dto.nombre());
        return mapearAResponse(tipoCostoRepository.save(tipo));
    }

    @Transactional
    public TipoCostoResponseDTO actualizar(Long id_tipo_costo, TipoCostoUpdateDTO dto) {
        TipoCosto tipo = obtenerEditable(id_tipo_costo);
        if (tipoCostoRepository.existsByNombreNormalizadoAndIdNot(
                TextoUtil.normalizarParaBusqueda(dto.nombre()), id_tipo_costo)) {
            throw new RecursoDuplicadoException("Ya existe un tipo de costo con ese nombre");
        }
        tipo.setNombre(dto.nombre());
        return mapearAResponse(tipo);
    }

    @Transactional
    public TipoCostoResponseDTO cambiarEstado(Long id_tipo_costo, boolean activo) {
        TipoCosto tipo = obtenerEditable(id_tipo_costo);
        tipo.setActivo(activo);
        return mapearAResponse(tipo);
    }

    private TipoCosto obtenerEditable(Long id_tipo_costo) {
        TipoCosto tipo = tipoCostoRepository.findById(id_tipo_costo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Tipo de costo no encontrado con id " + id_tipo_costo));
        if (tipo.isReservado()) {
            throw new OperacionNoPermitidaException("El tipo \"" + tipo.getNombre() + "\" es del sistema y no se puede modificar");
        }
        return tipo;
    }

    private TipoCostoResponseDTO mapearAResponse(TipoCosto t) {
        return new TipoCostoResponseDTO(t.getId_tipo_costo(), t.getNombre(), t.isActivo(), t.isReservado());
    }
}
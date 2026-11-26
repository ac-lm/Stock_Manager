package com.taller.gestion.catalogo.service;

import com.taller.gestion.catalogo.dto.InsumoCreateDTO;
import com.taller.gestion.catalogo.dto.InsumoResponseDTO;
import com.taller.gestion.catalogo.dto.InsumoUpdateDTO;
import com.taller.gestion.catalogo.entity.Insumo;
import com.taller.gestion.catalogo.repository.InsumoRepository;
import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.common.exception.RecursoNoEncontradoException;
import com.taller.gestion.stock.entity.MotivoMovimientoStock;
import com.taller.gestion.stock.entity.TipoMovimientoStock;
import com.taller.gestion.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InsumoService {

    private final InsumoRepository insumoRepository;

    // StockService usa InsumoRepository, no este servicio: no hay ciclo de dependencias.
    private final StockService stockService;

    @Transactional(readOnly = true)
    public List<InsumoResponseDTO> listarInsumos() {
        return insumoRepository.findAllByOrderByNombreAsc()
                .stream()
                .map(this::mapearAResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public InsumoResponseDTO obtenerInsumo(Long id_insumo) {
        return mapearAResponse(obtenerPorId(id_insumo));
    }

    /**
     * Devuelve la entidad para uso interno del módulo y de otros módulos
     * que necesiten asociar un insumo (siempre a través de este servicio).
     */
    @Transactional(readOnly = true)
    public Insumo obtenerPorId(Long id_insumo) {
        return insumoRepository.findById(id_insumo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Insumo no encontrado con ID: " + id_insumo));
    }

    @Transactional
    public InsumoResponseDTO crearInsumo(InsumoCreateDTO dto) {
        validarNombreYUnidad(dto.nombre(), dto.unidadMedida());

        BigDecimal stockInicial = dto.stockActual() != null ? dto.stockActual() : BigDecimal.ZERO;
        if (stockInicial.compareTo(BigDecimal.ZERO) < 0) {
            throw new DatosInvalidosException("El stock inicial no puede ser negativo");
        }

        Insumo insumo = new Insumo();
        insumo.setNombre(dto.nombre());
        insumo.setStockActual(BigDecimal.ZERO);
        insumo.setUnidadMedida(dto.unidadMedida());
        insumo.setStockMinimo(dto.stockMinimo() != null ? dto.stockMinimo() : 0);
        insumo.setCosto(dto.costo() != null ? dto.costo() : BigDecimal.ZERO);

        Insumo guardado = insumoRepository.save(insumo);
        if (stockInicial.compareTo(BigDecimal.ZERO) > 0) {
            stockService.registrarMovimiento(
                    null, guardado.getId_insumo(), TipoMovimientoStock.ENTRADA,
                    MotivoMovimientoStock.INICIAL, stockInicial, "Stock inicial del alta");
        }
        return mapearAResponse(guardado);
    }

    @Transactional
    public InsumoResponseDTO actualizarInsumo(Long id_insumo, InsumoUpdateDTO dto) {
        Insumo insumo = obtenerPorId(id_insumo);

        if (dto.nombre() != null && dto.nombre().isBlank()) {
            throw new DatosInvalidosException("El nombre del insumo no puede estar vacío");
        }
        if (dto.unidadMedida() != null && dto.unidadMedida().isBlank()) {
            throw new DatosInvalidosException("La unidad de medida del insumo no puede estar vacía");
        }

        if (dto.nombre() != null) {
            insumo.setNombre(dto.nombre());
        }
        if (dto.unidadMedida() != null) {
            insumo.setUnidadMedida(dto.unidadMedida());
        }
        if (dto.stockMinimo() != null) {
            insumo.setStockMinimo(dto.stockMinimo());
        }
        if (dto.costo() != null) {
            insumo.setCosto(dto.costo());
        }

        return mapearAResponse(insumoRepository.save(insumo));
    }

    @Transactional
    public void eliminarInsumo(Long id_insumo) {
        insumoRepository.delete(obtenerPorId(id_insumo));
    }

    private void validarNombreYUnidad(String nombre, String unidadMedida) {
        if (nombre == null || nombre.isBlank()) {
            throw new DatosInvalidosException("El nombre del insumo es obligatorio");
        }
        if (unidadMedida == null || unidadMedida.isBlank()) {
            throw new DatosInvalidosException("La unidad de medida del insumo es obligatoria");
        }
    }

    private InsumoResponseDTO mapearAResponse(Insumo insumo) {
        return new InsumoResponseDTO(
                insumo.getId_insumo(),
                insumo.getNombre(),
                insumo.getStockActual(),
                insumo.getUnidadMedida(),
                insumo.getStockMinimo(),
                insumo.getCosto()
        );
    }
}

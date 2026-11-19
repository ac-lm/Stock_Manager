package com.taller.gestion.costos.service;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.common.exception.OperacionNoPermitidaException;
import com.taller.gestion.common.exception.RecursoNoEncontradoException;
import com.taller.gestion.costos.dto.CostoAnularDTO;
import com.taller.gestion.costos.dto.CostoCreateDTO;
import com.taller.gestion.costos.dto.CostoResponseDTO;
import com.taller.gestion.costos.dto.CostoUpdateDTO;
import com.taller.gestion.costos.entity.Costo;
import com.taller.gestion.costos.entity.TipoCosto;
import com.taller.gestion.costos.repository.CostoRepository;
import com.taller.gestion.costos.repository.TipoCostoRepository;
import com.taller.gestion.usuario.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CostoService {
    private final CostoRepository costoRepository;
    private final TipoCostoRepository tipoCostoRepository;
    private final UsuarioService usuarioService;

    @Transactional(readOnly = true)
    public Page<CostoResponseDTO> buscar(Long id_tipo_costo, Integer mes, Integer anio,
                                         boolean incluirAnulados, Pageable pageable) {
        return costoRepository.buscar(id_tipo_costo, mes, anio, incluirAnulados, pageable)
                .map(this::mapearAResponse);
    }

    @Transactional(readOnly = true)
    public BigDecimal total(Long id_tipo_costo, Integer mes, Integer anio) {
        return costoRepository.total(id_tipo_costo, mes, anio);
    }

    @Transactional(readOnly = true)
    public CostoResponseDTO findById(Long id_costo) {
        return mapearAResponse(obtenerCosto(id_costo));
    }

    @Transactional
    public CostoResponseDTO crear(CostoCreateDTO dto) {
        TipoCosto tipo = obtenerTipoParaCargaManual(dto.id_tipo_costo());

        Costo costo = new Costo();
        costo.setTipoCosto(tipo);
        costo.setDescripcion(dto.descripcion());
        costo.setMonto(dto.monto());
        costo.setFechaPago(dto.fechaPago());
        costo.setPeriodoMes(dto.periodoMes());
        costo.setPeriodoAnio(dto.periodoAnio());
        costo.setObservaciones(dto.observaciones());
        costo.setUsuario(usuarioService.obtenerAutenticado());

        return mapearAResponse(costoRepository.save(costo));
    }

    @Transactional
    public CostoResponseDTO actualizar(Long id_costo, CostoUpdateDTO dto) {
        Costo costo = obtenerCostoModificable(id_costo);
        TipoCosto tipo = obtenerTipoParaCargaManual(dto.id_tipo_costo());

        costo.setTipoCosto(tipo);
        costo.setDescripcion(dto.descripcion());
        costo.setMonto(dto.monto());
        costo.setFechaPago(dto.fechaPago());
        costo.setPeriodoMes(dto.periodoMes());
        costo.setPeriodoAnio(dto.periodoAnio());
        costo.setObservaciones(dto.observaciones());
        // TODO (CST-08): auditar monto anterior y nuevo

        return mapearAResponse(costo);
    }

    @Transactional
    public CostoResponseDTO anular(Long id_costo, CostoAnularDTO dto) {
        Costo costo = obtenerCostoModificable(id_costo);
        // TODO (CST-05): si el costo está vinculado a un egreso de caja, rechazar con
        //                OperacionNoPermitidaException ("anulá primero el egreso")

        costo.setAnulado(true);
        costo.setMotivoAnulacion(dto.motivo());
        costo.setUsuarioAnulacion(usuarioService.obtenerAutenticado());
        costo.setFechaAnulacion(LocalDateTime.now());

        return mapearAResponse(costo);
    }

    // El tipo tiene que existir, estar activo y no ser "reservado" (insumos)
    private TipoCosto obtenerTipoParaCargaManual(Long id_tipo_costo) {
        TipoCosto tipo = tipoCostoRepository.findById(id_tipo_costo)
                .orElseThrow(() -> new DatosInvalidosException("El tipo de costo no existe"));
        if (tipo.isReservado()) {
            throw new DatosInvalidosException(
                    "Los costos de insumos se generan al ingresar mercadería");
        }
        if (!tipo.isActivo()) {
            throw new DatosInvalidosException("El tipo de costo está desactivado");
        }
        return tipo;
    }

    // No se puede editar ni anular un costo ya anulado, ni uno generado por un ingreso
    private Costo obtenerCostoModificable(Long id_costo) {
        Costo costo = obtenerCosto(id_costo);
        if (costo.isAnulado()) {
            throw new OperacionNoPermitidaException("El costo ya está anulado y no se puede modificar");
        }
        if (costo.getId_ingreso_mercaderia() != null) {
            throw new OperacionNoPermitidaException(
                    "Este costo lo generó un ingreso de mercadería: se anula anulando el ingreso");
        }
        return costo;
    }

    private Costo obtenerCosto(Long id_costo) {
        return costoRepository.findById(id_costo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Costo no encontrado con id " + id_costo));
    }

    private CostoResponseDTO mapearAResponse(Costo c) {
        return new CostoResponseDTO(
                c.getId_costo(),
                c.getTipoCosto().getId_tipo_costo(),
                c.getTipoCosto().getNombre(),
                c.getDescripcion(),
                c.getMonto(),
                c.getFechaPago(),
                c.getPeriodoMes(),
                c.getPeriodoAnio(),
                c.getUsuario() != null ? c.getUsuario().getId_usuario() : null,
                c.getId_ingreso_mercaderia(),
                c.getId_ingreso_mercaderia() != null,
                c.isAnulado(),
                c.getMotivoAnulacion(),
                c.getUsuarioAnulacion() != null ? c.getUsuarioAnulacion().getId_usuario() : null,
                c.getFechaAnulacion(),
                c.getObservaciones()
        );
    }
}
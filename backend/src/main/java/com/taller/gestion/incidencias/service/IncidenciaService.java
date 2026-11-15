package com.taller.gestion.incidencias.service;

import com.taller.gestion.catalogo.entity.Insumo;
import com.taller.gestion.catalogo.entity.Producto;
import com.taller.gestion.catalogo.service.InsumoService;
import com.taller.gestion.catalogo.service.ProductoService;
import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.common.exception.OperacionNoPermitidaException;
import com.taller.gestion.common.exception.RecursoNoEncontradoException;
import com.taller.gestion.incidencias.dto.IncidenciaCreateDTO;
import com.taller.gestion.incidencias.dto.IncidenciaResolucionDTO;
import com.taller.gestion.incidencias.dto.IncidenciaResponseDTO;
import com.taller.gestion.incidencias.dto.IncidenciaUpdateDTO;
import com.taller.gestion.incidencias.entity.EstadoIncidencia;
import com.taller.gestion.incidencias.entity.Incidencia;
import com.taller.gestion.incidencias.entity.TipoIncidencia;
import com.taller.gestion.incidencias.repository.IncidenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class IncidenciaService {

    private final IncidenciaRepository incidenciaRepository;
    private final TipoIncidenciaService tipoIncidenciaService;
    private final InsumoService insumoService;
    private final ProductoService productoService;

    @Transactional
    public IncidenciaResponseDTO crearIncidencia(IncidenciaCreateDTO dto) {
        if (dto.id_tipo_incidencia() == null) {
            throw new DatosInvalidosException("El tipo de incidencia es obligatorio");
        }
        validarAfectacion(dto.id_insumo(), dto.id_producto(), dto.cantidadAfectada());

        TipoIncidencia tipo = tipoIncidenciaService.obtenerActivoPorId(dto.id_tipo_incidencia());

        Incidencia incidencia = new Incidencia();
        incidencia.setTipoIncidencia(tipo);
        incidencia.setInsumo(dto.id_insumo() != null ? insumoService.obtenerPorId(dto.id_insumo()) : null);
        incidencia.setProducto(dto.id_producto() != null ? productoService.obtenerPorId(dto.id_producto()) : null);
        incidencia.setDescripcion(dto.descripcion());
        incidencia.setCantidadAfectada(dto.cantidadAfectada());
        incidencia.setObservaciones(dto.observaciones());
        incidencia.setId_usuario(dto.id_usuario());
        incidencia.setEstado(EstadoIncidencia.ABIERTA);
        incidencia.setFecha(LocalDateTime.now());

        return mapearAResponse(incidenciaRepository.save(incidencia));
    }

    @Transactional(readOnly = true)
    public Page<IncidenciaResponseDTO> listarIncidencias(
            EstadoIncidencia estado,
            Long id_tipo_incidencia,
            Long id_insumo,
            Long id_producto,
            LocalDateTime desde,
            LocalDateTime hasta,
            Pageable pageable) {
        return incidenciaRepository
                .buscar(estado, id_tipo_incidencia, id_insumo, id_producto, desde, hasta, pageable)
                .map(this::mapearAResponse);
    }

    @Transactional(readOnly = true)
    public IncidenciaResponseDTO obtenerIncidencia(Long id_incidencia) {
        return mapearAResponse(obtenerPorId(id_incidencia));
    }

    @Transactional
    public IncidenciaResponseDTO actualizarIncidencia(Long id_incidencia, IncidenciaUpdateDTO dto) {
        Incidencia incidencia = obtenerPorId(id_incidencia);

        if (incidencia.getEstado() == EstadoIncidencia.CERRADA) {
            throw new OperacionNoPermitidaException(
                    "No se puede modificar una incidencia cerrada");
        }
        if (dto.id_tipo_incidencia() == null) {
            throw new DatosInvalidosException("El tipo de incidencia es obligatorio");
        }
        validarAfectacion(dto.id_insumo(), dto.id_producto(), dto.cantidadAfectada());

        TipoIncidencia tipo = tipoIncidenciaService.obtenerActivoPorId(dto.id_tipo_incidencia());

        incidencia.setTipoIncidencia(tipo);
        incidencia.setInsumo(dto.id_insumo() != null ? insumoService.obtenerPorId(dto.id_insumo()) : null);
        incidencia.setProducto(dto.id_producto() != null ? productoService.obtenerPorId(dto.id_producto()) : null);
        incidencia.setDescripcion(dto.descripcion());
        incidencia.setCantidadAfectada(dto.cantidadAfectada());
        incidencia.setObservaciones(dto.observaciones());

        return mapearAResponse(incidenciaRepository.save(incidencia));
    }

    /**
     * Cierra la incidencia. El descuento de stock llega con INC-07, cuando exista
     * el módulo stock; por ahora solo registra la resolución.
     */
    @Transactional
    public IncidenciaResponseDTO resolverIncidencia(Long id_incidencia, IncidenciaResolucionDTO dto) {
        Incidencia incidencia = obtenerPorId(id_incidencia);

        if (incidencia.getEstado() == EstadoIncidencia.CERRADA) {
            throw new OperacionNoPermitidaException("La incidencia ya está cerrada");
        }
        if (dto.resolucionAdoptada() == null || dto.resolucionAdoptada().isBlank()) {
            throw new DatosInvalidosException("La resolución adoptada es obligatoria");
        }

        incidencia.setResolucionAdoptada(dto.resolucionAdoptada());
        incidencia.setId_usuario_resolucion(dto.id_usuario_resolucion());
        incidencia.setFechaResolucion(LocalDateTime.now());
        incidencia.setEstado(EstadoIncidencia.CERRADA);

        return mapearAResponse(incidenciaRepository.save(incidencia));
    }

    @Transactional
    public void eliminarIncidencia(Long id_incidencia) {
        Incidencia incidencia = obtenerPorId(id_incidencia);

        if (incidencia.getEstado() == EstadoIncidencia.CERRADA) {
            throw new OperacionNoPermitidaException(
                    "No se puede eliminar una incidencia cerrada");
        }

        incidenciaRepository.delete(incidencia);
    }

    private Incidencia obtenerPorId(Long id_incidencia) {
        return incidenciaRepository.findById(id_incidencia)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Incidencia no encontrada con ID: " + id_incidencia));
    }

    private void validarAfectacion(Long id_insumo, Long id_producto, BigDecimal cantidadAfectada) {
        if (id_insumo == null && id_producto == null) {
            throw new DatosInvalidosException(
                    "La incidencia debe afectar al menos un producto o un insumo");
        }
        if (cantidadAfectada == null || cantidadAfectada.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DatosInvalidosException("La cantidad afectada debe ser mayor a cero");
        }
    }

    private IncidenciaResponseDTO mapearAResponse(Incidencia incidencia) {
        TipoIncidencia tipo = incidencia.getTipoIncidencia();
        Insumo insumo = incidencia.getInsumo();
        Producto producto = incidencia.getProducto();

        return new IncidenciaResponseDTO(
                incidencia.getId_incidencia(),
                tipo != null ? tipo.getId_tipo_incidencia() : null,
                tipo != null ? tipo.getNombre() : null,
                insumo != null ? insumo.getId_insumo() : null,
                insumo != null ? insumo.getNombre() : null,
                producto != null ? producto.getId_producto() : null,
                producto != null ? producto.getNombre() : null,
                incidencia.getDescripcion(),
                incidencia.getCantidadAfectada(),
                incidencia.getEstado(),
                incidencia.getObservaciones(),
                incidencia.getId_usuario(),
                incidencia.getFecha(),
                incidencia.getId_usuario_resolucion(),
                incidencia.getFechaResolucion(),
                incidencia.getResolucionAdoptada()
        );
    }
}

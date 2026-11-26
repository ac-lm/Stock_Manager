package com.taller.gestion.stock.service;

import com.taller.gestion.catalogo.entity.Insumo;
import com.taller.gestion.catalogo.entity.Producto;
import com.taller.gestion.catalogo.repository.InsumoRepository;
import com.taller.gestion.catalogo.repository.ProductoRepository;
import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.common.exception.OperacionNoPermitidaException;
import com.taller.gestion.common.exception.RecursoNoEncontradoException;
import com.taller.gestion.stock.dto.MovimientoStockResponseDTO;
import com.taller.gestion.stock.dto.StockInsumoResponseDTO;
import com.taller.gestion.stock.dto.StockProductoResponseDTO;
import com.taller.gestion.stock.entity.MotivoMovimientoStock;
import com.taller.gestion.stock.entity.MovimientoStock;
import com.taller.gestion.stock.entity.TipoMovimientoStock;
import com.taller.gestion.stock.repository.MovimientoStockRepository;
import com.taller.gestion.usuario.entity.Usuario;
import com.taller.gestion.usuario.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Único punto por el que se modifica {@code stock_actual}. El movimiento y el cambio de saldo
 * se guardan en la misma transacción y la fila del ítem se bloquea con {@code PESSIMISTIC_WRITE}
 * para evitar condiciones de carrera.
 */
@Service
@RequiredArgsConstructor
public class StockService {

    private final MovimientoStockRepository movimientoStockRepository;
    private final ProductoRepository productoRepository;
    private final InsumoRepository insumoRepository;
    private final UsuarioService usuarioService;

    @Transactional(readOnly = true)
    public List<StockInsumoResponseDTO> listarInsumos() {
        return insumoRepository.findAllByOrderByNombreAsc().stream()
                .map(this::mapearInsumo)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StockProductoResponseDTO> listarProductos() {
        return productoRepository.findAllByOrderByNombreAsc().stream()
                .map(this::mapearProducto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MovimientoStockResponseDTO> kardexInsumo(Long id_insumo) {
        if (!insumoRepository.existsById(id_insumo)) {
            throw new RecursoNoEncontradoException("Insumo no encontrado con ID: " + id_insumo);
        }
        return movimientoStockRepository.listarPorInsumo(id_insumo).stream()
                .map(this::mapearMovimiento)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MovimientoStockResponseDTO> kardexProducto(Long id_producto) {
        if (!productoRepository.existsById(id_producto)) {
            throw new RecursoNoEncontradoException("Producto no encontrado con ID: " + id_producto);
        }
        return movimientoStockRepository.listarPorProducto(id_producto).stream()
                .map(this::mapearMovimiento)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<MovimientoStockResponseDTO> historial(
            TipoMovimientoStock tipo,
            MotivoMovimientoStock motivo,
            Long id_producto,
            Long id_insumo,
            Long id_usuario,
            LocalDateTime desde,
            LocalDateTime hasta,
            Pageable pageable) {
        return movimientoStockRepository.listar(
                        tipo, motivo, id_producto, id_insumo, id_usuario, desde, hasta, pageable)
                .map(this::mapearMovimiento);
    }

    /**
     * Registra un movimiento y actualiza el saldo. Es el único método que escribe
     * {@code stock_actual}. Lanzar la excepción antes de guardar hace que la transacción
     * haga rollback de todo, incluido el movimiento.
     */
    @Transactional
    public MovimientoStockResponseDTO registrarMovimiento(
            Long id_producto,
            Long id_insumo,
            TipoMovimientoStock tipo,
            MotivoMovimientoStock motivo,
            BigDecimal cantidad,
            String observaciones) {

        if (tipo == null) {
            throw new DatosInvalidosException("El tipo de movimiento es obligatorio");
        }
        if (motivo == null) {
            throw new DatosInvalidosException("El motivo del movimiento es obligatorio");
        }
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DatosInvalidosException("La cantidad debe ser mayor a cero");
        }
        if ((id_producto == null) == (id_insumo == null)) {
            throw new DatosInvalidosException(
                    "El movimiento debe indicar exactamente un producto o un insumo");
        }

        Usuario usuario = usuarioService.obtenerAutenticado();

        MovimientoStock movimiento = new MovimientoStock();
        movimiento.setTipo(tipo);
        movimiento.setMotivo(motivo);
        movimiento.setCantidad(cantidad);
        movimiento.setUsuario(usuario);
        movimiento.setFechaHora(LocalDateTime.now());
        movimiento.setObservaciones(
                observaciones == null || observaciones.isBlank() ? null : observaciones.trim());

        if (id_producto != null) {
            Producto producto = productoRepository.findByIdParaActualizar(id_producto)
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Producto no encontrado con ID: " + id_producto));
            BigDecimal saldo = saldoNoNulo(producto.getStockActual());
            producto.setStockActual(aplicarMovimiento(saldo, cantidad, tipo, "producto"));
            movimiento.setProducto(producto);
        } else {
            Insumo insumo = insumoRepository.findByIdParaActualizar(id_insumo)
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Insumo no encontrado con ID: " + id_insumo));
            BigDecimal saldo = saldoNoNulo(insumo.getStockActual());
            insumo.setStockActual(aplicarMovimiento(saldo, cantidad, tipo, "insumo"));
            movimiento.setInsumo(insumo);
        }

        return mapearMovimiento(movimientoStockRepository.save(movimiento));
    }

    // Métodos de uso interno para futuras integraciones (VEN-10, INC-07, CST-06).
    public MovimientoStockResponseDTO descontarProducto(
            Long id_producto, BigDecimal cantidad, MotivoMovimientoStock motivo) {
        return registrarMovimiento(
                id_producto, null, TipoMovimientoStock.SALIDA, motivo, cantidad, null);
    }

    public MovimientoStockResponseDTO ingresarProducto(
            Long id_producto, BigDecimal cantidad, MotivoMovimientoStock motivo) {
        return registrarMovimiento(
                id_producto, null, TipoMovimientoStock.ENTRADA, motivo, cantidad, null);
    }

    public MovimientoStockResponseDTO descontarInsumo(
            Long id_insumo, BigDecimal cantidad, MotivoMovimientoStock motivo) {
        return registrarMovimiento(
                null, id_insumo, TipoMovimientoStock.SALIDA, motivo, cantidad, null);
    }

    public MovimientoStockResponseDTO ingresarInsumo(
            Long id_insumo, BigDecimal cantidad, MotivoMovimientoStock motivo) {
        return registrarMovimiento(
                null, id_insumo, TipoMovimientoStock.ENTRADA, motivo, cantidad, null);
    }

    private BigDecimal aplicarMovimiento(
            BigDecimal saldo, BigDecimal cantidad, TipoMovimientoStock tipo, String nombreItem) {
        if (tipo == TipoMovimientoStock.ENTRADA) {
            return saldo.add(cantidad);
        }
        if (saldo.compareTo(cantidad) < 0) {
            throw new OperacionNoPermitidaException(
                    "Stock insuficiente para el " + nombreItem
                            + ". Saldo: " + saldo + ", se intentó descontar: " + cantidad);
        }
        return saldo.subtract(cantidad);
    }

    private BigDecimal saldoNoNulo(BigDecimal saldo) {
        return saldo != null ? saldo : BigDecimal.ZERO;
    }

    private StockInsumoResponseDTO mapearInsumo(Insumo insumo) {
        BigDecimal stock = saldoNoNulo(insumo.getStockActual());
        Integer minimo = insumo.getStockMinimo() != null ? insumo.getStockMinimo() : 0;
        return new StockInsumoResponseDTO(
                insumo.getId_insumo(),
                insumo.getNombre(),
                insumo.getUnidadMedida(),
                stock,
                minimo,
                stock.compareTo(BigDecimal.valueOf(minimo)) < 0);
    }

    private StockProductoResponseDTO mapearProducto(Producto producto) {
        return new StockProductoResponseDTO(
                producto.getId_producto(),
                producto.getNombre(),
                producto.getCategoria(),
                saldoNoNulo(producto.getStockActual()));
    }

    private MovimientoStockResponseDTO mapearMovimiento(MovimientoStock movimiento) {
        MovimientoStockResponseDTO resultado = new MovimientoStockResponseDTO(
                movimiento.getId_movimiento_stock(),
                movimiento.getProducto() != null ? movimiento.getProducto().getId_producto() : null,
                movimiento.getProducto() != null ? movimiento.getProducto().getNombre() : null,
                movimiento.getInsumo() != null ? movimiento.getInsumo().getId_insumo() : null,
                movimiento.getInsumo() != null ? movimiento.getInsumo().getNombre() : null,
                movimiento.getTipo(),
                movimiento.getMotivo(),
                movimiento.getCantidad(),
                movimiento.getUsuario() != null ? movimiento.getUsuario().getId_usuario() : null,
                movimiento.getUsuario() != null ? movimiento.getUsuario().getNombre() : null,
                movimiento.getFechaHora(),
                movimiento.getObservaciones(),
                movimiento.getVenta() != null ? movimiento.getVenta().getId_venta() : null
        );
        return resultado;
    }
}

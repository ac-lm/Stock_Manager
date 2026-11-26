package com.taller.gestion.venta.service;

import com.taller.gestion.caja.dto.CobroVentaDTO;
import com.taller.gestion.caja.entity.ConceptoMovimientoCaja;
import com.taller.gestion.caja.entity.EstadoSesionCaja;
import com.taller.gestion.caja.entity.MovimientoCaja;
import com.taller.gestion.caja.entity.SesionCaja;
import com.taller.gestion.caja.entity.TipoMovimientoCaja;
import com.taller.gestion.caja.repository.MovimientoCajaRepository;
import com.taller.gestion.caja.repository.SesionCajaRepository;
import com.taller.gestion.catalogo.entity.Producto;
import com.taller.gestion.catalogo.service.ProductoService;
import com.taller.gestion.clientes.dto.ClienteResponseDTO;
import com.taller.gestion.clientes.entity.Cliente;
import com.taller.gestion.clientes.entity.TipoCliente;
import com.taller.gestion.clientes.service.ClienteService;
import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.common.exception.OperacionNoPermitidaException;
import com.taller.gestion.common.exception.RecursoNoEncontradoException;
import com.taller.gestion.usuario.entity.Usuario;
import com.taller.gestion.usuario.service.UsuarioService;
import com.taller.gestion.venta.dto.DetalleVentaCreateDTO;
import com.taller.gestion.venta.dto.DetalleVentaResponseDTO;
import com.taller.gestion.venta.dto.VentaCreateDTO;
import com.taller.gestion.venta.dto.VentaResponseDTO;
import com.taller.gestion.venta.entity.DetalleVenta;
import com.taller.gestion.venta.entity.EstadoVenta;
import com.taller.gestion.venta.entity.PrecioTipoCliente;
import com.taller.gestion.venta.entity.Venta;
import com.taller.gestion.venta.repository.PrecioTipoClienteRepository;
import com.taller.gestion.venta.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VentaService {

    private final VentaRepository ventaRepository;
    private final PrecioTipoClienteRepository precioTipoClienteRepository;
    private final ClienteService clienteService;
    private final ProductoService productoService;
    private final UsuarioService usuarioService;
    private final SesionCajaRepository sesionCajaRepository;
    private final MovimientoCajaRepository movimientoCajaRepository;

    /** Datos del cliente que necesita el alta, sin importar si vino de la sesión o del body. */
    private record ClienteDeVenta(Long id_cliente, String nombre, TipoCliente tipo) {
    }

    private static final String SIN_CAJA_ABIERTA =
            "No hay una sesión de caja abierta. Abra la caja antes de confirmar una venta";

    /**
     * Arma y confirma la venta en un solo paso, junto con un ingreso de caja por cada pago.
     * El precio no se recibe: se resuelve desde la lista del tipo de cliente, con fallback al
     * precio de catálogo mientras no haya filas. No se descuenta stock: esa responsabilidad
     * queda para VEN-10, cuando exista el módulo.
     */
    @Transactional
    public VentaResponseDTO crearVenta(VentaCreateDTO dto) {
        if (dto.detalles() == null || dto.detalles().isEmpty()) {
            throw new DatosInvalidosException("La venta debe tener al menos un producto");
        }
        validarDetalles(dto.detalles());
        if (dto.pagos() == null || dto.pagos().isEmpty()) {
            throw new DatosInvalidosException("La venta debe tener al menos un pago");
        }
        validarPagos(dto.pagos());

        SesionCaja sesion = obtenerSesionAbierta();
        ClienteDeVenta cliente = resolveCliente(dto.id_cliente());
        Usuario vendedor = usuarioService.obtenerAutenticado();

        Venta venta = new Venta();
        venta.setId_cliente(cliente.id_cliente());
        venta.setId_vendedor(vendedor.getId_usuario());
        venta.setFecha(LocalDate.now());
        venta.setHora(LocalTime.now());
        venta.setEstado(EstadoVenta.CONFIRMADA);
        venta.setFechaVencimiento(dto.fechaVencimiento());

        BigDecimal total = BigDecimal.ZERO;
        for (DetalleVentaCreateDTO item : dto.detalles()) {
            Producto producto = productoService.obtenerPorId(item.id_producto_fk());
            BigDecimal precio = resolvePrecio(producto, cliente.tipo());

            DetalleVenta detalle = new DetalleVenta();
            detalle.setId_producto_fk(item.id_producto_fk());
            detalle.setCantidad(item.cantidad());
            detalle.setPrecioHistorico(precio);
            detalle.setSubtotal(precio.multiply(item.cantidad()).setScale(2, RoundingMode.HALF_UP));
            venta.agregarDetalle(detalle);

            total = total.add(detalle.getSubtotal());
        }
        venta.setTotal(total.setScale(2, RoundingMode.HALF_UP));

        BigDecimal totalPagos = dto.pagos().stream()
                .map(CobroVentaDTO::monto)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        if (totalPagos.compareTo(venta.getTotal()) != 0) {
            throw new DatosInvalidosException(
                    "La suma de los pagos ($" + totalPagos.toPlainString()
                            + ") debe igualar el total de la venta ($"
                            + venta.getTotal().toPlainString() + ")");
        }

        Venta guardada = ventaRepository.save(venta);
        registrarPagos(sesion, vendedor, guardada, dto.pagos());

        return mapearAResponse(guardada);
    }

    @Transactional(readOnly = true)
    public VentaResponseDTO obtenerVenta(Long id_venta) {
        return mapearAResponse(obtenerPorId(id_venta));
    }

    @Transactional(readOnly = true)
    public Page<VentaResponseDTO> listarVentas(
            EstadoVenta estado, Long id_cliente, LocalDate desde, LocalDate hasta, Pageable pageable) {
        return ventaRepository.buscar(estado, id_cliente, desde, hasta, pageable)
                .map(this::mapearAResponse);
    }

    /** Anula la venta sin borrarla. No hay reingreso de stock porque la venta nunca lo descontó. */
    @Transactional
    public VentaResponseDTO anularVenta(Long id_venta) {
        Venta venta = obtenerPorId(id_venta);
        if (venta.getEstado() == EstadoVenta.ANULADA) {
            throw new OperacionNoPermitidaException("La venta ya está anulada");
        }
        venta.setEstado(EstadoVenta.ANULADA);
        return mapearAResponse(ventaRepository.save(venta));
    }

    private void validarDetalles(List<DetalleVentaCreateDTO> detalles) {
        for (DetalleVentaCreateDTO item : detalles) {
            if (item == null || item.id_producto_fk() == null) {
                throw new DatosInvalidosException("El id del producto es obligatorio");
            }
            if (item.cantidad() == null || item.cantidad().compareTo(BigDecimal.ZERO) <= 0) {
                throw new DatosInvalidosException("La cantidad debe ser mayor a cero");
            }
        }
    }

    private void validarPagos(List<CobroVentaDTO> pagos) {
        for (CobroVentaDTO pago : pagos) {
            if (pago == null || pago.medioPago() == null) {
                throw new DatosInvalidosException("El medio de pago es obligatorio");
            }
            if (pago.monto() == null || pago.monto().compareTo(BigDecimal.ZERO) <= 0) {
                throw new DatosInvalidosException("El monto del pago debe ser mayor a cero");
            }
            if (pago.monto().scale() > 2) {
                throw new DatosInvalidosException("El monto del pago admite hasta 2 decimales");
            }
        }
    }

    /** Bloquea la sesión para que no se cierre mientras se registran los cobros. */
    private SesionCaja obtenerSesionAbierta() {
        Long id_sesion = sesionCajaRepository
                .findFirstByEstadoOrderByFechaAperturaDescHoraAperturaDesc(EstadoSesionCaja.ABIERTA)
                .map(SesionCaja::getId_sesion)
                .orElseThrow(() -> new OperacionNoPermitidaException(SIN_CAJA_ABIERTA));

        SesionCaja sesion = sesionCajaRepository.findByIdForUpdate(id_sesion)
                .orElseThrow(() -> new OperacionNoPermitidaException(SIN_CAJA_ABIERTA));
        if (sesion.getEstado() != EstadoSesionCaja.ABIERTA) {
            throw new OperacionNoPermitidaException("La sesión de caja ya no está abierta");
        }
        return sesion;
    }

    private void registrarPagos(SesionCaja sesion, Usuario vendedor, Venta venta,
                                List<CobroVentaDTO> pagos) {
        for (CobroVentaDTO pago : pagos) {
            MovimientoCaja movimiento = new MovimientoCaja();
            movimiento.setSesionCaja(sesion);
            movimiento.setUsuario(vendedor);
            movimiento.setId_venta(venta.getId_venta());
            movimiento.setTipo(TipoMovimientoCaja.INGRESO);
            movimiento.setConcepto(ConceptoMovimientoCaja.VENTA);
            movimiento.setMonto(pago.monto().setScale(2, RoundingMode.HALF_UP));
            movimiento.setMedioPago(pago.medioPago());
            movimiento.setFecha(venta.getFecha());
            movimiento.setHora(venta.getHora());
            movimiento.setObservaciones("Cobro de la venta #" + venta.getId_venta());
            movimientoCajaRepository.save(movimiento);
        }
    }

    private ClienteDeVenta resolveCliente(Long id_cliente) {
        if (id_cliente == null) {
            Cliente generico = clienteService.obtenerGenerico();
            return new ClienteDeVenta(generico.getId_cliente(), generico.getNombre(),
                    generico.getTipoCliente());
        }

        ClienteResponseDTO cliente = clienteService.findById(id_cliente);
        if (!cliente.activo()) {
            throw new OperacionNoPermitidaException("No se puede vender a un cliente inactivo");
        }
        return new ClienteDeVenta(cliente.id_cliente(), cliente.nombre(), cliente.tipoCliente());
    }

    private BigDecimal resolvePrecio(Producto producto, TipoCliente tipo) {
        return precioTipoClienteRepository
                .findByIdProductoAndTipoCliente(producto.getId_producto(), tipo)
                .map(PrecioTipoCliente::getPrecio)
                .orElseGet(() -> producto.getPrecio() != null ? producto.getPrecio() : BigDecimal.ZERO);
    }

    private Venta obtenerPorId(Long id_venta) {
        return ventaRepository.findById(id_venta)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Venta no encontrada con id " + id_venta));
    }

    private VentaResponseDTO mapearAResponse(Venta venta) {
        List<DetalleVentaResponseDTO> detalles = venta.getDetalles().stream()
                .map(d -> new DetalleVentaResponseDTO(
                        d.getId_detalle_venta(),
                        d.getVenta() != null ? d.getVenta().getId_venta() : null,
                        d.getId_producto_fk(),
                        d.getCantidad(),
                        d.getPrecioHistorico(),
                        d.getSubtotal()))
                .toList();
        List<CobroVentaDTO> pagos = venta.getId_venta() == null
                ? List.of()
                : movimientoCajaRepository
                        .listarPorVentaYTipo(venta.getId_venta(), TipoMovimientoCaja.INGRESO)
                        .stream()
                        .map(m -> new CobroVentaDTO(m.getMedioPago(), m.getMonto()))
                        .toList();
        return new VentaResponseDTO(
                venta.getId_venta(),
                venta.getId_cliente(),
                resolveNombreCliente(venta.getId_cliente()),
                venta.getId_vendedor(),
                venta.getFecha(),
                venta.getHora(),
                venta.getTotal(),
                venta.getEstado(),
                venta.getFechaVencimiento(),
                detalles,
                pagos);
    }

    /**
     * El nombre es informativo: si el cliente se eliminó o el servicio no lo encuentra, la venta
     * se muestra igual con el id. No puede fallar una consulta por un dato de solo lectura.
     */
    private String resolveNombreCliente(Long id_cliente) {
        if (id_cliente == null) {
            return null;
        }
        try {
            ClienteResponseDTO cliente = clienteService.findById(id_cliente);
            return cliente != null ? cliente.nombre() : null;
        } catch (RecursoNoEncontradoException ex) {
            return null;
        }
    }
}

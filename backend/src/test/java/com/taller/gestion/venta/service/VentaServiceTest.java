package com.taller.gestion.venta.service;

import com.taller.gestion.caja.dto.CobroVentaDTO;
import com.taller.gestion.caja.entity.ConceptoMovimientoCaja;
import com.taller.gestion.caja.entity.EstadoSesionCaja;
import com.taller.gestion.caja.entity.MedioPago;
import com.taller.gestion.caja.entity.MovimientoCaja;
import com.taller.gestion.caja.entity.SesionCaja;
import com.taller.gestion.caja.entity.TipoMovimientoCaja;
import com.taller.gestion.caja.repository.MovimientoCajaRepository;
import com.taller.gestion.caja.repository.SesionCajaRepository;
import com.taller.gestion.caja.service.CajaService;
import com.taller.gestion.catalogo.entity.Producto;
import com.taller.gestion.catalogo.service.ProductoService;
import com.taller.gestion.clientes.dto.ClienteResponseDTO;
import com.taller.gestion.clientes.entity.Cliente;
import com.taller.gestion.clientes.entity.TipoCliente;
import com.taller.gestion.clientes.service.ClienteService;
import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.common.exception.OperacionNoPermitidaException;
import com.taller.gestion.common.exception.RecursoNoEncontradoException;
import com.taller.gestion.stock.service.StockService;
import com.taller.gestion.usuario.entity.Usuario;
import com.taller.gestion.usuario.service.UsuarioService;
import com.taller.gestion.venta.dto.AnularVentaDTO;
import com.taller.gestion.venta.dto.DetalleVentaCreateDTO;
import com.taller.gestion.venta.dto.VentaCreateDTO;
import com.taller.gestion.venta.dto.VentaResponseDTO;
import com.taller.gestion.venta.entity.EstadoVenta;
import com.taller.gestion.venta.entity.PrecioTipoCliente;
import com.taller.gestion.venta.entity.Venta;
import com.taller.gestion.venta.repository.PrecioTipoClienteRepository;
import com.taller.gestion.venta.repository.VentaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    private static final Long ID_GENERICO = 1L;
    private static final Long ID_PRODUCTO = 10L;
    private static final Long ID_VENDEDOR = 5L;
    private static final Long ID_SESION = 7L;
    private static final Long ID_VENTA = 1L;

    @Mock
    private VentaRepository ventaRepository;
    @Mock
    private PrecioTipoClienteRepository precioTipoClienteRepository;
    @Mock
    private ClienteService clienteService;
    @Mock
    private ProductoService productoService;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private SesionCajaRepository sesionCajaRepository;
    @Mock
    private MovimientoCajaRepository movimientoCajaRepository;
    @Mock
    private CajaService cajaService;
    @Mock
    private StockService stockService;

    @InjectMocks
    private VentaService ventaService;

    private Cliente generico() {
        Cliente c = new Cliente();
        c.setId_cliente(ID_GENERICO);
        c.setNombre("Consumidor final");
        c.setTipoCliente(TipoCliente.GENERICO);
        return c;
    }

    private ClienteResponseDTO genericoResponse() {
        return new ClienteResponseDTO(ID_GENERICO, null, "Consumidor final", null,
                "generico@stockmanager.local", "Sin dirección",
                TipoCliente.GENERICO, null, true, null);
    }

    private Producto producto() {
        Producto p = new Producto();
        p.setId_producto(ID_PRODUCTO);
        p.setNombre("Taza");
        p.setPrecio(new BigDecimal("100.00"));
        p.setStockActual(new BigDecimal("0.000"));
        return p;
    }

    private Usuario vendedor() {
        Usuario u = new Usuario();
        u.setId_usuario(ID_VENDEDOR);
        return u;
    }

    private SesionCaja sesionAbierta() {
        SesionCaja s = new SesionCaja();
        s.setId_sesion(ID_SESION);
        s.setEstado(EstadoSesionCaja.ABIERTA);
        return s;
    }

    private CobroVentaDTO pago(MedioPago medio, String monto) {
        return new CobroVentaDTO(medio, new BigDecimal(monto));
    }

    private VentaCreateDTO dto(Long id_cliente, String cantidad, List<CobroVentaDTO> pagos) {
        return new VentaCreateDTO(id_cliente, null,
                List.of(new DetalleVentaCreateDTO(ID_PRODUCTO, new BigDecimal(cantidad))), pagos);
    }

    private VentaCreateDTO dto(Long id_cliente, String cantidad) {
        return dto(id_cliente, cantidad, List.of(pago(MedioPago.EFECTIVO, "250.00")));
    }

    private void prepararGenerico() {
        when(clienteService.obtenerGenerico()).thenReturn(generico());
        when(clienteService.findById(ID_GENERICO)).thenReturn(genericoResponse());
    }

    private void prepararVendedorYProducto() {
        when(usuarioService.obtenerAutenticado()).thenReturn(vendedor());
        when(productoService.obtenerPorId(ID_PRODUCTO)).thenReturn(producto());
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> {
            Venta v = inv.getArgument(0);
            v.setId_venta(ID_VENTA);
            return v;
        });
    }

    /** Se usa lenient porque algunos tests fallan antes de llegar a la caja. */
    private void prepararCaja() {
        SesionCaja sesion = sesionAbierta();
        lenient().when(sesionCajaRepository
                        .findFirstByEstadoOrderByFechaAperturaDescHoraAperturaDesc(
                                EstadoSesionCaja.ABIERTA))
                .thenReturn(Optional.of(sesion));
        lenient().when(sesionCajaRepository.findByIdForUpdate(ID_SESION))
                .thenReturn(Optional.of(sesion));
    }

    private MovimientoCaja movimiento(MedioPago medio, String monto) {
        MovimientoCaja m = new MovimientoCaja();
        m.setMedioPago(medio);
        m.setMonto(new BigDecimal(monto));
        return m;
    }

    // ============================ ALTA ============================

    @Test
    @DisplayName("Alta de mostrador: usa el genérico, el vendedor de sesión y precio de catálogo")
    void crear_mostrador() {
        prepararGenerico();
        prepararVendedorYProducto();
        prepararCaja();
        when(movimientoCajaRepository.listarPorVentaYTipo(ID_VENTA, TipoMovimientoCaja.INGRESO))
                .thenReturn(List.of(movimiento(MedioPago.EFECTIVO, "250.00")));

        VentaResponseDTO r = ventaService.crearVenta(dto(null, "2.500"));

        assertThat(r.id_venta()).isEqualTo(ID_VENTA);
        assertThat(r.cliente()).isEqualTo("Consumidor final");
        assertThat(r.id_cliente()).isEqualTo(ID_GENERICO);
        assertThat(r.id_vendedor()).isEqualTo(ID_VENDEDOR);
        assertThat(r.estado()).isEqualTo(EstadoVenta.CONFIRMADA);
        assertThat(r.detalles()).hasSize(1);
        assertThat(r.detalles().get(0).precioHistorico()).isEqualByComparingTo("100.00");
        assertThat(r.detalles().get(0).subtotal()).isEqualByComparingTo("250.00");
        assertThat(r.detalles().get(0).id_venta_fk()).isEqualTo(ID_VENTA);
        assertThat(r.total()).isEqualByComparingTo("250.00");
        assertThat(r.pagos()).hasSize(1);
        assertThat(r.pagos().get(0).medioPago()).isEqualTo(MedioPago.EFECTIVO);
        assertThat(r.pagos().get(0).monto()).isEqualByComparingTo("250.00");
    }

    @Test
    @DisplayName("Alta con lista de precios: usa el precio del tipo de cliente, no el de catálogo")
    void crear_conListaDePrecios() {
        ClienteResponseDTO granCliente = new ClienteResponseDTO(2L, "20-12345678-6", "Panadería Sol",
                null, "sol@mail.com", "Calle 1", TipoCliente.GRAN_CLIENTE, null, true, 30);
        when(clienteService.findById(2L)).thenReturn(granCliente);
        when(precioTipoClienteRepository.findByIdProductoAndTipoCliente(
                ID_PRODUCTO, TipoCliente.GRAN_CLIENTE))
                .thenReturn(Optional.of(new PrecioTipoCliente(1L, ID_PRODUCTO,
                        TipoCliente.GRAN_CLIENTE, new BigDecimal("80.00"))));
        prepararVendedorYProducto();
        prepararCaja();

        VentaResponseDTO r = ventaService.crearVenta(
                dto(2L, "2.500", List.of(pago(MedioPago.EFECTIVO, "200.00"))));

        assertThat(r.detalles().get(0).precioHistorico()).isEqualByComparingTo("80.00");
        assertThat(r.detalles().get(0).subtotal()).isEqualByComparingTo("200.00");
        assertThat(r.total()).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("Pago mixto: registra un INGRESO/VENTA por medio con el id de la venta")
    void crear_pagoMixto_registraMovimientos() {
        prepararGenerico();
        prepararVendedorYProducto();
        prepararCaja();

        ventaService.crearVenta(dto(null, "2.500", List.of(
                pago(MedioPago.EFECTIVO, "100.00"),
                pago(MedioPago.TRANSFERENCIA, "150.00"))));

        ArgumentCaptor<MovimientoCaja> captor = ArgumentCaptor.forClass(MovimientoCaja.class);
        verify(movimientoCajaRepository, times(2)).save(captor.capture());
        List<MovimientoCaja> movimientos = captor.getAllValues();
        assertThat(movimientos).extracting(MovimientoCaja::getTipo)
                .containsOnly(TipoMovimientoCaja.INGRESO);
        assertThat(movimientos).extracting(MovimientoCaja::getConcepto)
                .containsOnly(ConceptoMovimientoCaja.VENTA);
        assertThat(movimientos).extracting(MovimientoCaja::getId_venta)
                .containsOnly(ID_VENTA);
        assertThat(movimientos).extracting(m -> m.getSesionCaja().getId_sesion())
                .containsOnly(ID_SESION);
        assertThat(movimientos).extracting(m -> m.getUsuario().getId_usuario())
                .containsOnly(ID_VENDEDOR);
        assertThat(movimientos).extracting(MovimientoCaja::getMedioPago)
                .containsExactly(MedioPago.EFECTIVO, MedioPago.TRANSFERENCIA);
        assertThat(movimientos).extracting(MovimientoCaja::getMonto)
                .containsExactly(new BigDecimal("100.00"), new BigDecimal("150.00"));
        assertThat(movimientos).extracting(MovimientoCaja::getObservaciones)
                .containsExactly("Cobro de la venta #1", "Cobro de la venta #1");
    }

    @Test
    @DisplayName("Sin pagos: falla y no guarda nada")
    void crear_sinPagos_falla() {
        VentaCreateDTO dto = dto(null, "2.500", List.of());

        assertThatThrownBy(() -> ventaService.crearVenta(dto))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("al menos un pago");
        verify(ventaRepository, never()).save(any());
        verify(movimientoCajaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Pago con monto cero: falla antes de consultar la caja")
    void crear_pagoCero_falla() {
        VentaCreateDTO dto = dto(null, "2.500",
                List.of(pago(MedioPago.EFECTIVO, "0.00")));

        assertThatThrownBy(() -> ventaService.crearVenta(dto))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("mayor a cero");
        verify(ventaRepository, never()).save(any());
        verify(sesionCajaRepository, never())
                .findFirstByEstadoOrderByFechaAperturaDescHoraAperturaDesc(any());
    }

    @Test
    @DisplayName("Pago sin medio: falla")
    void crear_pagoSinMedio_falla() {
        VentaCreateDTO dto = dto(null, "2.500",
                List.of(new CobroVentaDTO(null, new BigDecimal("250.00"))));

        assertThatThrownBy(() -> ventaService.crearVenta(dto))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("medio de pago");
    }

    @Test
    @DisplayName("Sin caja abierta: no se confirma la venta")
    void crear_sinCajaAbierta_falla() {
        when(sesionCajaRepository.findFirstByEstadoOrderByFechaAperturaDescHoraAperturaDesc(
                EstadoSesionCaja.ABIERTA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ventaService.crearVenta(dto(null, "2.500")))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("caja abierta");
        verify(ventaRepository, never()).save(any());
        verify(movimientoCajaRepository, never()).save(any());
    }

    @Test
    @DisplayName("La sesión se cierra entre la búsqueda y el bloqueo: no se confirma")
    void crear_sesionCerradaAlBloquear_falla() {
        SesionCaja cerrada = sesionAbierta();
        cerrada.setEstado(EstadoSesionCaja.CERRADA);
        when(sesionCajaRepository.findFirstByEstadoOrderByFechaAperturaDescHoraAperturaDesc(
                EstadoSesionCaja.ABIERTA)).thenReturn(Optional.of(sesionAbierta()));
        when(sesionCajaRepository.findByIdForUpdate(ID_SESION)).thenReturn(Optional.of(cerrada));

        assertThatThrownBy(() -> ventaService.crearVenta(dto(null, "2.500")))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("ya no está abierta");
        verify(ventaRepository, never()).save(any());
        verify(movimientoCajaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Pagos que no suman el total: falla y no guarda nada")
    void crear_sumaPagosDistinta_falla() {
        when(clienteService.obtenerGenerico()).thenReturn(generico());
        when(usuarioService.obtenerAutenticado()).thenReturn(vendedor());
        when(productoService.obtenerPorId(ID_PRODUCTO)).thenReturn(producto());
        prepararCaja();

        VentaCreateDTO dto = dto(null, "2.500", List.of(
                pago(MedioPago.EFECTIVO, "100.00"),
                pago(MedioPago.TRANSFERENCIA, "100.00")));

        assertThatThrownBy(() -> ventaService.crearVenta(dto))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("suma de los pagos")
                .hasMessageContaining("250.00");
        verify(ventaRepository, never()).save(any());
        verify(movimientoCajaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Alta sin detalles: falla")
    void crear_sinDetalles_falla() {
        VentaCreateDTO dto = new VentaCreateDTO(null, null, List.of(),
                List.of(pago(MedioPago.EFECTIVO, "250.00")));

        assertThatThrownBy(() -> ventaService.crearVenta(dto))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("al menos un producto");
        verify(ventaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Alta con cantidad cero: falla")
    void crear_cantidadCero_falla() {
        assertThatThrownBy(() -> ventaService.crearVenta(dto(null, "0")))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("mayor a cero");
        verify(ventaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Alta a cliente inactivo: no se permite")
    void crear_clienteInactivo_falla() {
        ClienteResponseDTO inactivo = new ClienteResponseDTO(3L, null, "Viejo", null,
                "viejo@mail.com", "Calle 2", TipoCliente.CONSUMIDOR_FINAL, null, false, null);
        when(clienteService.findById(3L)).thenReturn(inactivo);
        prepararCaja();

        assertThatThrownBy(() -> ventaService.crearVenta(dto(3L, "1")))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("inactivo");
        verify(ventaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Alta con fecha de vencimiento: se persiste")
    void crear_conFechaVencimiento() {
        prepararGenerico();
        prepararVendedorYProducto();
        prepararCaja();
        LocalDate vencimiento = LocalDate.of(2026, 12, 15);
        VentaCreateDTO dto = new VentaCreateDTO(null, vencimiento,
                List.of(new DetalleVentaCreateDTO(ID_PRODUCTO, BigDecimal.ONE)),
                List.of(pago(MedioPago.EFECTIVO, "100.00")));

        VentaResponseDTO r = ventaService.crearVenta(dto);

        assertThat(r.fechaVencimiento()).isEqualTo(vencimiento);
    }

    // ======================= CONSULTAS Y ANULACIÓN =======================

    private Venta ventaConfirmada() {
        Venta venta = new Venta();
        venta.setId_venta(ID_VENTA);
        venta.setEstado(EstadoVenta.CONFIRMADA);
        venta.setId_cliente(ID_GENERICO);
        return venta;
    }

    @Test
    @DisplayName("Anular una confirmada revierte cobros y stock y cambia el estado")
    void anular_confirmada() {
        Venta venta = ventaConfirmada();
        when(ventaRepository.findById(ID_VENTA)).thenReturn(Optional.of(venta));
        when(ventaRepository.save(venta)).thenReturn(venta);

        VentaResponseDTO r = ventaService.anularVenta(
                ID_VENTA, new AnularVentaDTO("  Producto defectuoso  "));

        assertThat(r.estado()).isEqualTo(EstadoVenta.ANULADA);
        verify(cajaService).anularCobrosDeVenta(ID_VENTA, "Producto defectuoso");
        verify(stockService).devolverStockDeVenta(ID_VENTA, "Producto defectuoso");
    }

    @Test
    @DisplayName("Anular dos veces: no se permite ni se revierte nada")
    void anular_yaAnulada_falla() {
        Venta venta = ventaConfirmada();
        venta.setEstado(EstadoVenta.ANULADA);
        when(ventaRepository.findById(ID_VENTA)).thenReturn(Optional.of(venta));

        assertThatThrownBy(() -> ventaService.anularVenta(
                ID_VENTA, new AnularVentaDTO("Motivo")))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("ya está anulada");
        verify(ventaRepository, never()).save(any());
        verify(cajaService, never()).anularCobrosDeVenta(any(), any());
        verify(stockService, never()).devolverStockDeVenta(any(), any());
    }

    @Test
    @DisplayName("Anular sin motivo: falla antes de tocar caja, stock o la venta")
    void anular_sinMotivo_falla() {
        assertThatThrownBy(() -> ventaService.anularVenta(
                ID_VENTA, new AnularVentaDTO("   ")))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("motivo");
        verify(ventaRepository, never()).findById(any());
        verify(cajaService, never()).anularCobrosDeVenta(any(), any());
        verify(stockService, never()).devolverStockDeVenta(any(), any());
    }

    @Test
    @DisplayName("Si falla la caja, la venta no se marca como anulada")
    void anular_fallaCaja_noMarcaAnulada() {
        Venta venta = ventaConfirmada();
        when(ventaRepository.findById(ID_VENTA)).thenReturn(Optional.of(venta));
        doThrow(new OperacionNoPermitidaException("Sin caja abierta"))
                .when(cajaService).anularCobrosDeVenta(eq(ID_VENTA), any());

        assertThatThrownBy(() -> ventaService.anularVenta(
                ID_VENTA, new AnularVentaDTO("Motivo")))
                .isInstanceOf(OperacionNoPermitidaException.class);
        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.CONFIRMADA);
        verify(ventaRepository, never()).save(any());
        verify(stockService, never()).devolverStockDeVenta(any(), any());
    }

    @Test
    @DisplayName("Si falla el stock, la venta no se marca como anulada")
    void anular_fallaStock_noMarcaAnulada() {
        Venta venta = ventaConfirmada();
        when(ventaRepository.findById(ID_VENTA)).thenReturn(Optional.of(venta));
        doThrow(new OperacionNoPermitidaException("Fallo el stock"))
                .when(stockService).devolverStockDeVenta(eq(ID_VENTA), any());

        assertThatThrownBy(() -> ventaService.anularVenta(
                ID_VENTA, new AnularVentaDTO("Motivo")))
                .isInstanceOf(OperacionNoPermitidaException.class);
        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.CONFIRMADA);
        verify(ventaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Obtener un id inexistente: no encontrado")
    void obtener_inexistente() {
        when(ventaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ventaService.obtenerVenta(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("Listar pasa los filtros al repositorio")
    void listar_filtros() {
        when(ventaRepository.buscar(eq(EstadoVenta.CONFIRMADA), eq(2L), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        Page<VentaResponseDTO> r = ventaService.listarVentas(
                EstadoVenta.CONFIRMADA, 2L, LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31), PageRequest.of(0, 20));

        assertThat(r).isEmpty();
        verify(ventaRepository).buscar(eq(EstadoVenta.CONFIRMADA), eq(2L), any(), any(), any());
    }
}

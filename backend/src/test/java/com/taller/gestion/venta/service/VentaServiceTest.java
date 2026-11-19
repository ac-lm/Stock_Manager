package com.taller.gestion.venta.service;

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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    private static final Long ID_GENERICO = 1L;
    private static final Long ID_PRODUCTO = 10L;
    private static final Long ID_VENDEDOR = 5L;

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

    @InjectMocks
    private VentaService ventaService;

    private Cliente generico() {
        Cliente c = new Cliente();
        c.setId_cliente(ID_GENERICO);
        c.setNombre("Consumidor final");
        c.setTipoCliente(TipoCliente.GENERICO);
        return c;
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

    private VentaCreateDTO dto(Long id_cliente, String cantidad) {
        return new VentaCreateDTO(id_cliente, null,
                List.of(new DetalleVentaCreateDTO(ID_PRODUCTO, new BigDecimal(cantidad))));
    }

    private void prepararGenerico() {
        when(clienteService.obtenerGenerico()).thenReturn(generico());
    }

    private void prepararVendedorYProducto() {
        when(usuarioService.obtenerAutenticado()).thenReturn(vendedor());
        when(productoService.obtenerPorId(ID_PRODUCTO)).thenReturn(producto());
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> {
            Venta v = inv.getArgument(0);
            v.setId_venta(1L);
            return v;
        });
    }

    // ============================ ALTA ============================

    @Test
    @DisplayName("Alta de mostrador: usa el genérico, el vendedor de sesión y precio de catálogo")
    void crear_mostrador() {
        prepararGenerico();
        when(clienteService.findById(ID_GENERICO)).thenReturn(
                new ClienteResponseDTO(ID_GENERICO, null, "Consumidor final", null,
                        "generico@stockmanager.local", "Sin dirección",
                        TipoCliente.GENERICO, null, true, null));
        prepararVendedorYProducto();

        VentaResponseDTO r = ventaService.crearVenta(dto(null, "2.500"));

        assertThat(r.id_venta()).isEqualTo(1L);
        assertThat(r.cliente()).isEqualTo("Consumidor final");
        assertThat(r.id_cliente()).isEqualTo(ID_GENERICO);
        assertThat(r.id_vendedor()).isEqualTo(ID_VENDEDOR);
        assertThat(r.estado()).isEqualTo(EstadoVenta.CONFIRMADA);
        assertThat(r.detalles()).hasSize(1);
        assertThat(r.detalles().get(0).precioHistorico()).isEqualByComparingTo("100.00");
        assertThat(r.detalles().get(0).subtotal()).isEqualByComparingTo("250.00");
        assertThat(r.detalles().get(0).id_venta_fk()).isEqualTo(1L);
        assertThat(r.total()).isEqualByComparingTo("250.00");
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

        VentaResponseDTO r = ventaService.crearVenta(dto(2L, "2.500"));

        assertThat(r.detalles().get(0).precioHistorico()).isEqualByComparingTo("80.00");
        assertThat(r.detalles().get(0).subtotal()).isEqualByComparingTo("200.00");
        assertThat(r.total()).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("Alta sin detalles: falla")
    void crear_sinDetalles_falla() {
        VentaCreateDTO dto = new VentaCreateDTO(null, null, List.of());

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
        LocalDate vencimiento = LocalDate.of(2026, 12, 15);
        VentaCreateDTO dto = new VentaCreateDTO(null, vencimiento,
                List.of(new DetalleVentaCreateDTO(ID_PRODUCTO, BigDecimal.ONE)));

        VentaResponseDTO r = ventaService.crearVenta(dto);

        assertThat(r.fechaVencimiento()).isEqualTo(vencimiento);
    }

    // ======================= CONSULTAS Y ANULACIÓN =======================

    @Test
    @DisplayName("Anular una confirmada cambia el estado a ANULADA")
    void anular_confirmada() {
        Venta venta = new Venta();
        venta.setId_venta(1L);
        venta.setEstado(EstadoVenta.CONFIRMADA);
        venta.setId_cliente(ID_GENERICO);
        when(ventaRepository.findById(1L)).thenReturn(Optional.of(venta));
        when(ventaRepository.save(venta)).thenReturn(venta);

        VentaResponseDTO r = ventaService.anularVenta(1L);

        assertThat(r.estado()).isEqualTo(EstadoVenta.ANULADA);
    }

    @Test
    @DisplayName("Anular dos veces: no se permite")
    void anular_yaAnulada_falla() {
        Venta venta = new Venta();
        venta.setId_venta(1L);
        venta.setEstado(EstadoVenta.ANULADA);
        when(ventaRepository.findById(1L)).thenReturn(Optional.of(venta));

        assertThatThrownBy(() -> ventaService.anularVenta(1L))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("ya está anulada");
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

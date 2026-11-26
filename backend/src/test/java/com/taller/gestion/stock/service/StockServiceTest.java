package com.taller.gestion.stock.service;

import com.taller.gestion.catalogo.entity.Insumo;
import com.taller.gestion.catalogo.entity.Producto;
import com.taller.gestion.catalogo.repository.InsumoRepository;
import com.taller.gestion.catalogo.repository.ProductoRepository;
import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.common.exception.OperacionNoPermitidaException;
import com.taller.gestion.common.exception.RecursoNoEncontradoException;
import com.taller.gestion.stock.dto.MovimientoStockResponseDTO;
import com.taller.gestion.stock.entity.MotivoMovimientoStock;
import com.taller.gestion.stock.entity.MovimientoStock;
import com.taller.gestion.stock.entity.TipoMovimientoStock;
import com.taller.gestion.stock.repository.MovimientoStockRepository;
import com.taller.gestion.usuario.entity.Rol;
import com.taller.gestion.usuario.entity.Usuario;
import com.taller.gestion.usuario.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private MovimientoStockRepository movimientoStockRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private InsumoRepository insumoRepository;
    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private StockService stockService;

    private Usuario usuario() {
        Usuario u = new Usuario();
        u.setId_usuario(7L);
        u.setNombre("Lautaro");
        u.setRol(Rol.VENDEDOR);
        u.setActivo(true);
        return u;
    }

    private Producto producto(String stock) {
        Producto p = new Producto();
        p.setId_producto(1L);
        p.setNombre("Producto de prueba");
        p.setStockActual(new BigDecimal(stock));
        return p;
    }

    private Insumo insumo(String stock) {
        Insumo i = new Insumo();
        i.setId_insumo(2L);
        i.setNombre("Insumo de prueba");
        i.setStockActual(new BigDecimal(stock));
        i.setStockMinimo(5);
        return i;
    }

    private void prepararGuardado() {
        when(movimientoStockRepository.save(any())).thenAnswer(invocation -> {
            MovimientoStock movimiento = invocation.getArgument(0);
            movimiento.setId_movimiento_stock(100L);
            return movimiento;
        });
    }

    @Test
    @DisplayName("Una entrada suma al saldo y guarda el movimiento con usuario y fecha")
    void entrada_sumaAlSaldo() {
        Producto p = producto("10.000");
        when(usuarioService.obtenerAutenticado()).thenReturn(usuario());
        when(productoRepository.findByIdParaActualizar(1L)).thenReturn(Optional.of(p));
        prepararGuardado();

        MovimientoStockResponseDTO resultado = stockService.registrarMovimiento(
                1L, null, TipoMovimientoStock.ENTRADA, MotivoMovimientoStock.COMPRA,
                new BigDecimal("3.500"), "  Reposición  ");

        assertThat(p.getStockActual()).isEqualByComparingTo("13.500");
        assertThat(resultado.id_producto()).isEqualTo(1L);
        assertThat(resultado.tipo()).isEqualTo(TipoMovimientoStock.ENTRADA);
        assertThat(resultado.usuario()).isEqualTo("Lautaro");
        assertThat(resultado.fecha_hora()).isNotNull();

        ArgumentCaptor<MovimientoStock> captor = ArgumentCaptor.forClass(MovimientoStock.class);
        verify(movimientoStockRepository).save(captor.capture());
        assertThat(captor.getValue().getObservaciones()).isEqualTo("Reposición");
        assertThat(captor.getValue().getMotivo()).isEqualTo(MotivoMovimientoStock.COMPRA);
        assertThat(captor.getValue().getInsumo()).isNull();
    }

    @Test
    @DisplayName("Una salida con saldo suficiente descuenta el stock del insumo")
    void salida_suficiente_descuenta() {
        Insumo i = insumo("10.000");
        when(usuarioService.obtenerAutenticado()).thenReturn(usuario());
        when(insumoRepository.findByIdParaActualizar(2L)).thenReturn(Optional.of(i));
        prepararGuardado();

        stockService.registrarMovimiento(
                null, 2L, TipoMovimientoStock.SALIDA, MotivoMovimientoStock.MERMA,
                new BigDecimal("2.500"), null);

        assertThat(i.getStockActual()).isEqualByComparingTo("7.500");
    }

    @Test
    @DisplayName("Una salida mayor al saldo lanza 409 y no guarda nada")
    void salida_insuficiente_noGuardaNada() {
        Producto p = producto("2.000");
        when(usuarioService.obtenerAutenticado()).thenReturn(usuario());
        when(productoRepository.findByIdParaActualizar(1L)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> stockService.registrarMovimiento(
                1L, null, TipoMovimientoStock.SALIDA, MotivoMovimientoStock.AJUSTE,
                new BigDecimal("3.000"), null))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("Stock insuficiente");

        assertThat(p.getStockActual()).isEqualByComparingTo("2.000");
        verify(movimientoStockRepository, never()).save(any());
    }

    @Test
    @DisplayName("El saldo nunca queda negativo: descontar todo deja cero")
    void salida_porElTotal_dejaCero() {
        Producto p = producto("5.000");
        when(usuarioService.obtenerAutenticado()).thenReturn(usuario());
        when(productoRepository.findByIdParaActualizar(1L)).thenReturn(Optional.of(p));
        prepararGuardado();

        stockService.registrarMovimiento(
                1L, null, TipoMovimientoStock.SALIDA, MotivoMovimientoStock.AJUSTE,
                new BigDecimal("5.000"), null);

        assertThat(p.getStockActual()).isEqualByComparingTo("0.000");
    }

    @Test
    @DisplayName("Rechaza un movimiento con producto e insumo a la vez")
    void productoYInsumo_juntos_seRechaza() {
        assertThatThrownBy(() -> stockService.registrarMovimiento(
                1L, 2L, TipoMovimientoStock.ENTRADA, MotivoMovimientoStock.AJUSTE,
                BigDecimal.ONE, null))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("exactamente un producto o un insumo");

        verify(movimientoStockRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rechaza un movimiento sin ítem")
    void sinItem_seRechaza() {
        assertThatThrownBy(() -> stockService.registrarMovimiento(
                null, null, TipoMovimientoStock.ENTRADA, MotivoMovimientoStock.AJUSTE,
                BigDecimal.ONE, null))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    @DisplayName("Rechaza cantidad cero o negativa")
    void cantidadNoPositiva_seRechaza() {
        assertThatThrownBy(() -> stockService.registrarMovimiento(
                1L, null, TipoMovimientoStock.ENTRADA, MotivoMovimientoStock.AJUSTE,
                BigDecimal.ZERO, null))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("mayor a cero");
    }

    @Test
    @DisplayName("Un id inexistente devuelve 404 y no registra el movimiento")
    void itemInexistente_lanzaNoEncontrado() {
        when(usuarioService.obtenerAutenticado()).thenReturn(usuario());
        when(productoRepository.findByIdParaActualizar(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.registrarMovimiento(
                99L, null, TipoMovimientoStock.ENTRADA, MotivoMovimientoStock.AJUSTE,
                BigDecimal.ONE, null))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(movimientoStockRepository, never()).save(any());
    }
}

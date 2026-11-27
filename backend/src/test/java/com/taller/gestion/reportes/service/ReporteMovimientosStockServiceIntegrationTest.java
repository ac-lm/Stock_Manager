package com.taller.gestion.reportes.service;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.MovimientoStockFilaDTO;
import com.taller.gestion.reportes.dto.ReporteMovimientosStockDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
class ReporteMovimientosStockServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ReporteMovimientosStockService service;

    private void guardarUsuario(long id) {
        jdbcTemplate.update(
                // El id 1 ya puede existir por el seed del proyecto: el reporte solo necesita
                // un usuario válido como FK, no crearlo de nuevo.
                "INSERT INTO Usuario (id_usuario, nombre, email, password, rol, activo) "
                        + "VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (id_usuario) DO NOTHING",
                id, "Ana", "ana" + id + "@stock.local", "x", "ADMINISTRADOR", true);
    }

    private void guardarVenta(long id) {
        jdbcTemplate.update(
                "INSERT INTO Venta (id_venta, id_cliente, id_vendedor, fecha, hora, total, estado) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                id, 900L, 1L, LocalDate.of(2026, 11, 12), java.sql.Time.valueOf("10:00:00"),
                new BigDecimal("50.00"), "CONFIRMADA");
    }

    private void guardarProducto(long id, String nombre) {
        jdbcTemplate.update(
                "INSERT INTO Producto (id_producto, nombre, precio, stock_actual, categoria) "
                        + "VALUES (?, ?, ?, ?, ?)",
                id, nombre, new BigDecimal("100.00"), BigDecimal.ZERO, "Prueba");
    }

    private void guardarInsumo(long id, String nombre, String unidad) {
        jdbcTemplate.update(
                "INSERT INTO Insumo (id_insumo, nombre, stock_actual, unidad_medida, "
                        + "stock_minimo, costo) VALUES (?, ?, ?, ?, ?, ?)",
                id, nombre, BigDecimal.ZERO, unidad, 1, BigDecimal.ZERO);
    }

    private void guardarMovimiento(long id, Long idProducto, Long idInsumo, String tipo,
                                   String cantidad, String motivo, LocalDateTime fechaHora,
                                   Long idVenta) {
        jdbcTemplate.update(
                "INSERT INTO Movimiento_Stock (id_movimiento_stock, id_producto, id_insumo, "
                        + "tipo, cantidad, motivo, id_usuario, fecha_hora, id_venta) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, idProducto, idInsumo, tipo, new BigDecimal(cantidad), motivo, 1L,
                Timestamp.valueOf(fechaHora), idVenta);
    }

    private void guardarProveedor(long id, String nombre) {
        jdbcTemplate.update(
                "INSERT INTO Proveedor (id_proveedor, nombre, CUIT, telefono, email, tipo_producto) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                id, nombre, "20-" + id + "-1", "555", "prov" + id + "@mail.local", "Insumos");
    }

    private void guardarProveedorInsumo(long id, long idProveedor, long idInsumo) {
        jdbcTemplate.update(
                "INSERT INTO Proveedor_Insumo (id_proveedor_insumo, id_proveedor, id_insumo) "
                        + "VALUES (?, ?, ?)",
                id, idProveedor, idInsumo);
    }

    private ReporteMovimientosStockDTO reporte() {
        return service.movimientosStock(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                null, null, null, null);
    }

    private MovimientoStockFilaDTO fila(ReporteMovimientosStockDTO reporte, String articulo) {
        return reporte.filas().stream()
                .filter(f -> f.articulo().equals(articulo))
                .findFirst()
                .orElseThrow();
    }

    @Test
    @Transactional
    @DisplayName("Clasifica entradas, venta, descartes, ajustes y otras salidas sin perder filas")
    void clasificacionDeColumnas() {
        guardarUsuario(1L);
        guardarVenta(1L);
        guardarProducto(1L, "Café");
        guardarMovimiento(1L, 1L, null, "ENTRADA", "5", "INICIAL",
                LocalDateTime.of(2026, 11, 10, 9, 0), null);
        guardarMovimiento(2L, 1L, null, "ENTRADA", "3", "COMPRA",
                LocalDateTime.of(2026, 11, 11, 9, 0), null);
        guardarMovimiento(3L, 1L, null, "ENTRADA", "1", "DEVOLUCION",
                LocalDateTime.of(2026, 11, 12, 9, 0), 1L);
        // Venta tiene prioridad aunque el motivo sea AJUSTE.
        guardarMovimiento(4L, 1L, null, "SALIDA", "2", "AJUSTE",
                LocalDateTime.of(2026, 11, 12, 10, 0), 1L);
        guardarMovimiento(5L, 1L, null, "SALIDA", "1", "MERMA",
                LocalDateTime.of(2026, 11, 13, 10, 0), null);
        guardarMovimiento(6L, 1L, null, "SALIDA", "0.500", "AJUSTE",
                LocalDateTime.of(2026, 11, 14, 10, 0), null);
        guardarMovimiento(7L, 1L, null, "SALIDA", "0.250", "COMPRA",
                LocalDateTime.of(2026, 11, 15, 10, 0), null);
        guardarMovimiento(8L, 1L, null, "SALIDA", "0.125", "DEVOLUCION",
                LocalDateTime.of(2026, 11, 16, 10, 0), null);

        ReporteMovimientosStockDTO reporte = reporte();

        assertThat(reporte.filas()).hasSize(1);
        MovimientoStockFilaDTO cafe = fila(reporte, "Café");
        assertThat(cafe.tipo().name()).isEqualTo("PRODUCTO");
        assertThat(cafe.unidad()).isNull();
        assertThat(cafe.entradas()).isEqualByComparingTo("9.000");
        assertThat(cafe.salidasVenta()).isEqualByComparingTo("2.000");
        assertThat(cafe.descartes()).isEqualByComparingTo("1.000");
        assertThat(cafe.ajustes()).isEqualByComparingTo("0.500");
        assertThat(cafe.otrasSalidas()).isEqualByComparingTo("0.375");
        assertThat(reporte.totales().articulos()).isEqualTo(1);
        assertThat(reporte.totales().movimientos()).isEqualTo(8);
    }

    @Test
    @Transactional
    @DisplayName("Agrupa por artículo: cada fila conserva su unidad y no se suman unidades distintas")
    void agrupaPorArticulo() {
        guardarUsuario(1L);
        guardarInsumo(10L, "Harina", "kg");
        guardarInsumo(11L, "Leche", "lt");
        guardarProducto(20L, "Mate");
        guardarMovimiento(10L, null, 10L, "ENTRADA", "4", "COMPRA",
                LocalDateTime.of(2026, 11, 5, 9, 0), null);
        guardarMovimiento(11L, null, 10L, "SALIDA", "1", "MERMA",
                LocalDateTime.of(2026, 11, 6, 9, 0), null);
        guardarMovimiento(12L, null, 11L, "ENTRADA", "10", "COMPRA",
                LocalDateTime.of(2026, 11, 7, 9, 0), null);
        guardarMovimiento(13L, 20L, null, "ENTRADA", "2", "INICIAL",
                LocalDateTime.of(2026, 11, 8, 9, 0), null);

        ReporteMovimientosStockDTO reporte = reporte();

        assertThat(reporte.filas()).extracting(MovimientoStockFilaDTO::articulo)
                .containsExactly("Harina", "Leche", "Mate");
        assertThat(fila(reporte, "Harina").unidad()).isEqualTo("kg");
        assertThat(fila(reporte, "Harina").entradas()).isEqualByComparingTo("4");
        assertThat(fila(reporte, "Harina").descartes()).isEqualByComparingTo("1");
        assertThat(fila(reporte, "Leche").unidad()).isEqualTo("lt");
        assertThat(fila(reporte, "Leche").entradas()).isEqualByComparingTo("10");
        assertThat(fila(reporte, "Mate").unidad()).isNull();
        assertThat(reporte.totales().articulos()).isEqualTo(3);
        assertThat(reporte.totales().movimientos()).isEqualTo(4);
    }

    @Test
    @Transactional
    @DisplayName("El rango incluye el primer y el último día completos, y excluye el día siguiente")
    void rangoInclusivo() {
        guardarUsuario(1L);
        guardarInsumo(10L, "Harina", "kg");
        guardarMovimiento(20L, null, 10L, "ENTRADA", "1", "COMPRA",
                LocalDateTime.of(2026, 11, 1, 0, 0), null);
        guardarMovimiento(21L, null, 10L, "ENTRADA", "2", "COMPRA",
                LocalDateTime.of(2026, 11, 30, 23, 59, 59), null);
        guardarMovimiento(22L, null, 10L, "ENTRADA", "99", "COMPRA",
                LocalDateTime.of(2026, 12, 1, 0, 0), null);

        ReporteMovimientosStockDTO reporte = reporte();

        assertThat(reporte.filas()).hasSize(1);
        assertThat(fila(reporte, "Harina").entradas()).isEqualByComparingTo("3");
        assertThat(reporte.totales().movimientos()).isEqualTo(2);
    }

    @Test
    @Transactional
    @DisplayName("Los filtros de artículo y tipo acotan la tabla y sus conteos")
    void filtrosDeArticuloYTipo() {
        guardarUsuario(1L);
        guardarInsumo(10L, "Harina", "kg");
        guardarProducto(20L, "Café");
        guardarMovimiento(30L, null, 10L, "ENTRADA", "4", "COMPRA",
                LocalDateTime.of(2026, 11, 5, 9, 0), null);
        guardarMovimiento(31L, null, 10L, "SALIDA", "1", "MERMA",
                LocalDateTime.of(2026, 11, 6, 9, 0), null);
        guardarMovimiento(32L, 20L, null, "ENTRADA", "2", "INICIAL",
                LocalDateTime.of(2026, 11, 7, 9, 0), null);

        ReporteMovimientosStockDTO insumo = service.movimientosStock(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                "INSUMO", 10L, null, null);
        assertThat(insumo.filas()).extracting(MovimientoStockFilaDTO::articulo).containsExactly("Harina");
        assertThat(insumo.tipoArticulo()).isEqualTo("INSUMO");
        assertThat(insumo.idArticulo()).isEqualTo(10L);

        ReporteMovimientosStockDTO salidas = service.movimientosStock(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                "INSUMO", 10L, "SALIDA", null);
        assertThat(salidas.tipoMovimiento()).isEqualTo("SALIDA");
        assertThat(fila(salidas, "Harina").entradas()).isEqualByComparingTo("0");
        assertThat(fila(salidas, "Harina").descartes()).isEqualByComparingTo("1");
        assertThat(salidas.totales().movimientos()).isEqualTo(1);

        assertThatThrownBy(() -> service.movimientosStock(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                "TODOS", 10L, null, null))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El artículo requiere indicar si es producto o insumo");
    }

    @Test
    @Transactional
    @DisplayName("El proveedor filtra solo insumos y no duplica movimientos con varios proveedores")
    void filtroPorProveedor() {
        guardarUsuario(1L);
        guardarInsumo(10L, "Harina", "kg");
        guardarProducto(20L, "Café");
        guardarProveedor(1L, "Proveedor A");
        guardarProveedor(2L, "Proveedor B");
        guardarProveedorInsumo(1L, 1L, 10L);
        guardarProveedorInsumo(2L, 2L, 10L);
        guardarMovimiento(40L, null, 10L, "ENTRADA", "4", "COMPRA",
                LocalDateTime.of(2026, 11, 5, 9, 0), null);
        guardarMovimiento(41L, null, 10L, "ENTRADA", "2", "COMPRA",
                LocalDateTime.of(2026, 11, 6, 9, 0), null);
        guardarMovimiento(42L, 20L, null, "ENTRADA", "1", "COMPRA",
                LocalDateTime.of(2026, 11, 7, 9, 0), null);

        ReporteMovimientosStockDTO conProveedor = service.movimientosStock(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                null, null, null, 1L);
        assertThat(conProveedor.filas()).extracting(MovimientoStockFilaDTO::articulo)
                .containsExactly("Harina");
        assertThat(conProveedor.idProveedor()).isEqualTo(1L);
        assertThat(fila(conProveedor, "Harina").entradas()).isEqualByComparingTo("6");
        assertThat(conProveedor.totales().movimientos()).isEqualTo(2);

        assertThat(service.movimientosStock(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                null, null, null, 99L).filas()).isEmpty();
    }

    @Test
    @Transactional
    @DisplayName("Un rango inválido o un tipo desconocido se responde como error de datos")
    void parametrosInvalidos() {
        assertThatThrownBy(() -> service.movimientosStock(
                null, LocalDate.of(2026, 11, 30), null, null, null, null))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El rango de fechas es obligatorio");
        assertThatThrownBy(() -> service.movimientosStock(
                LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 1),
                null, null, null, null))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("La fecha 'desde' no puede ser posterior a 'hasta'");
        assertThatThrownBy(() -> service.movimientosStock(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                "BEBIDA", null, null, null))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El parámetro 'tipo_articulo' tiene un valor inválido");
        assertThatThrownBy(() -> service.movimientosStock(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                null, null, "DESCARTE", null))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El parámetro 'tipo_movimiento' tiene un valor inválido");
    }
}

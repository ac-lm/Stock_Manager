package com.taller.gestion.reportes.service;

import com.taller.gestion.reportes.dto.ReporteVentasPorProductoDTO;
import com.taller.gestion.reportes.dto.VentasPorProductoFilaDTO;
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
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
class ReporteVentasPorProductoServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ReporteVentasPorProductoService service;

    private void guardarProducto(long id, String nombre, String categoria) {
        jdbcTemplate.update(
                "INSERT INTO Producto (id_producto, nombre, precio, stock_actual, categoria) "
                        + "VALUES (?, ?, ?, ?, ?)",
                id, nombre, new BigDecimal("999.99"), new BigDecimal("10.000"), categoria);
    }

    private long guardarVenta(LocalDate fecha, String estado, String total) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO Venta (id_cliente, id_vendedor, fecha, hora, total, estado) "
                        + "VALUES (?, ?, ?, ?, ?, ?) RETURNING id_venta",
                Long.class,
                2L, 5L, Date.valueOf(fecha), Time.valueOf(LocalTime.of(10, 30)),
                new BigDecimal(total), estado);
    }

    private void guardarDetalle(long idVenta, long idProducto, String cantidad, String subtotal) {
        jdbcTemplate.update(
                "INSERT INTO Detalle_Venta (id_venta_fk, id_producto_fk, cantidad, "
                        + "precio_historico, subtotal) VALUES (?, ?, ?, ?, ?)",
                idVenta, idProducto, new BigDecimal(cantidad), new BigDecimal("999.99"),
                new BigDecimal(subtotal));
    }

    private void cargarDatos() {
        guardarProducto(1L, "Café", "Bebidas");
        guardarProducto(2L, "Torta", "Repostería");
        guardarProducto(3L, "Mate", "Bebidas");

        long primera = guardarVenta(LocalDate.of(2026, 10, 1), "CONFIRMADA", "150.00");
        guardarDetalle(primera, 1L, "3.000", "150.00");

        long segunda = guardarVenta(LocalDate.of(2026, 10, 2), "CONFIRMADA", "300.00");
        guardarDetalle(segunda, 2L, "2.000", "300.00");

        // Anulada: ni la cantidad ni el subtotal deben contar.
        long anulada = guardarVenta(LocalDate.of(2026, 10, 3), "ANULADA", "999.00");
        guardarDetalle(anulada, 1L, "9.000", "999.00");

        // El último día cuenta completo, aunque el precio actual del catálogo sea otro.
        long ultimoDia = guardarVenta(LocalDate.of(2026, 10, 31), "CONFIRMADA", "370.00");
        guardarDetalle(ultimoDia, 1L, "0.500", "50.00");
        guardarDetalle(ultimoDia, 3L, "1.000", "320.00");

        long fueraDeRango = guardarVenta(LocalDate.of(2026, 11, 1), "CONFIRMADA", "500.00");
        guardarDetalle(fueraDeRango, 2L, "5.000", "500.00");
    }

    private VentasPorProductoFilaDTO fila(ReporteVentasPorProductoDTO reporte, String producto) {
        return reporte.filas().stream()
                .filter(f -> f.producto().equals(producto))
                .findFirst()
                .orElseThrow();
    }

    @Test
    @Transactional
    @DisplayName("Suma subtotales de las líneas confirmadas, no el precio actual del catálogo")
    void ventasPorProducto_soloConfirmadasYSubtotalesHistoricos() {
        cargarDatos();

        ReporteVentasPorProductoDTO reporte = service.ventasPorProducto(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), null, null, 10);

        assertThat(reporte.filas()).hasSize(3);
        assertThat(fila(reporte, "Café").cantidadVendida()).isEqualByComparingTo("3.500");
        assertThat(fila(reporte, "Café").totalVendido()).isEqualByComparingTo("200.00");
        assertThat(fila(reporte, "Café").categoria()).isEqualTo("Bebidas");
        assertThat(fila(reporte, "Torta").totalVendido()).isEqualByComparingTo("300.00");
        assertThat(fila(reporte, "Mate").totalVendido()).isEqualByComparingTo("320.00");
        assertThat(reporte.totales().cantidadVendida()).isEqualByComparingTo("6.500");
        assertThat(reporte.totales().totalVendido()).isEqualByComparingTo("820.00");
        assertThat(reporte.totales().productos()).isEqualTo(3);
    }

    @Test
    @Transactional
    @DisplayName("Ranking por total vendido, con el tope elegido")
    void ranking_respetaElTopeElegido() {
        cargarDatos();

        ReporteVentasPorProductoDTO reporte = service.ventasPorProducto(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), null, null, 2);

        assertThat(reporte.top()).isEqualTo(2);
        assertThat(reporte.ranking()).extracting("producto").containsExactly("Mate", "Torta");
        assertThat(reporte.filas()).hasSize(3);
    }

    @Test
    @Transactional
    @DisplayName("Filtros por producto y por categoría se aplican a la misma consulta")
    void filtrosPorProductoYCategoria() {
        cargarDatos();

        ReporteVentasPorProductoDTO porCategoria = service.ventasPorProducto(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), null, "Bebidas", 10);
        assertThat(porCategoria.filas()).extracting("producto").containsExactly("Mate", "Café");
        assertThat(porCategoria.totales().totalVendido()).isEqualByComparingTo("520.00");
        assertThat(porCategoria.totales().productos()).isEqualTo(2);

        ReporteVentasPorProductoDTO porProducto = service.ventasPorProducto(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), 1L, null, 10);
        assertThat(porProducto.filas()).extracting("producto").containsExactly("Café");
        assertThat(porProducto.totales().totalVendido()).isEqualByComparingTo("200.00");

        ReporteVentasPorProductoDTO sinCoincidencias = service.ventasPorProducto(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), 2L, "Bebidas", 10);
        assertThat(sinCoincidencias.filas()).isEmpty();
        assertThat(sinCoincidencias.ranking()).isEmpty();
    }

    @Test
    @Transactional
    @DisplayName("Rango sin ventas: tabla y ranking vacíos, totales en cero")
    void rangoSinVentas() {
        cargarDatos();

        ReporteVentasPorProductoDTO reporte = service.ventasPorProducto(
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 3), null, null, 10);

        assertThat(reporte.filas()).isEmpty();
        assertThat(reporte.ranking()).isEmpty();
        assertThat(reporte.totales().cantidadVendida()).isEqualByComparingTo("0.000");
        assertThat(reporte.totales().totalVendido()).isEqualByComparingTo("0.00");
    }
}

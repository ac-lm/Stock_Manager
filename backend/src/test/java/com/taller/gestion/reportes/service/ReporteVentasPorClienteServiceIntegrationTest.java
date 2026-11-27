package com.taller.gestion.reportes.service;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.ReporteVentasPorClienteDTO;
import com.taller.gestion.reportes.dto.TotalesPorTipoClienteDTO;
import com.taller.gestion.reportes.dto.VentasPorClienteFilaDTO;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
class ReporteVentasPorClienteServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ReporteVentasPorClienteService service;

    private record Clientes(long gran, long registrado, long inactivo, long mostrador) {
    }

    private long guardarCliente(long id, String nombre, String tipo, boolean activo) {
        jdbcTemplate.update(
                "INSERT INTO Cliente (id_cliente, CUIT, nombre, telefono, email, direccion, "
                        + "tipo_cliente, observaciones, activo, plazo_pago_dias, nombre_normalizado) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, null, nombre, null, "cliente" + id + "@taller.local", "Calle 1",
                tipo, null, activo, null, nombre.toLowerCase());
        return id;
    }

    private long guardarVenta(LocalDate fecha, String estado, String total, long idCliente) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO Venta (id_cliente, id_vendedor, fecha, hora, total, estado) "
                        + "VALUES (?, ?, ?, ?, ?, ?) RETURNING id_venta",
                Long.class,
                idCliente, 5L, Date.valueOf(fecha), Time.valueOf(LocalTime.of(10, 30)),
                new BigDecimal(total), estado);
    }

    private Clientes cargarDatos() {
        long gran = guardarCliente(101L, "Distribuidora Sol", "GRAN_CLIENTE", true);
        long registrado = guardarCliente(102L, "Ana Pérez", "CONSUMIDOR_FINAL", true);
        long inactivo = guardarCliente(103L, "Cliente viejo", "GRAN_CLIENTE", false);
        long mostrador = jdbcTemplate.queryForObject(
                "SELECT id_cliente FROM Cliente WHERE tipo_cliente = 'GENERICO' "
                        + "ORDER BY id_cliente LIMIT 1", Long.class);
        assertThat(mostrador).as("cliente genérico creado al arrancar").isNotNull();

        guardarVenta(LocalDate.of(2026, 10, 1), "CONFIRMADA", "100.00", gran);
        guardarVenta(LocalDate.of(2026, 10, 2), "CONFIRMADA", "50.00", registrado);
        guardarVenta(LocalDate.of(2026, 10, 2), "CONFIRMADA", "200.00", mostrador);
        guardarVenta(LocalDate.of(2026, 10, 3), "CONFIRMADA", "75.00", inactivo);
        // Ni esta venta anulada ni la del mes siguiente pueden entrar en el reporte.
        guardarVenta(LocalDate.of(2026, 10, 4), "ANULADA", "999.00", gran);
        guardarVenta(LocalDate.of(2026, 10, 31), "CONFIRMADA", "25.00", gran);
        guardarVenta(LocalDate.of(2026, 11, 1), "CONFIRMADA", "500.00", gran);

        return new Clientes(gran, registrado, inactivo, mostrador);
    }

    private ReporteVentasPorClienteDTO reporte() {
        return service.ventasPorCliente(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), null, null);
    }

    private VentasPorClienteFilaDTO fila(ReporteVentasPorClienteDTO reporte, String cliente) {
        return reporte.filas().stream()
                .filter(f -> f.cliente().equals(cliente))
                .findFirst()
                .orElseThrow();
    }

    private TotalesPorTipoClienteDTO porTipo(ReporteVentasPorClienteDTO reporte, String tipo) {
        return reporte.porTipo().stream()
                .filter(t -> t.tipo_cliente().equals(tipo))
                .findFirst()
                .orElseThrow();
    }

    @Test
    @Transactional
    @DisplayName("Solo cuentan las ventas confirmadas del rango, inclusive el último día")
    void soloConfirmadasYUltimoDiaIncluido() {
        cargarDatos();

        ReporteVentasPorClienteDTO reporte = reporte();

        assertThat(reporte.filas()).hasSize(4);
        assertThat(reporte.totales().cantidadCompras()).isEqualTo(5);
        assertThat(reporte.totales().totalVendido()).isEqualByComparingTo("450.00");
        assertThat(reporte.totales().clientes()).isEqualTo(4);
        assertThat(reporte.filas()).noneMatch(f -> f.totalVendido().compareTo(new BigDecimal("999.00")) == 0);
        assertThat(reporte.filas()).noneMatch(f -> f.totalVendido().compareTo(new BigDecimal("500.00")) == 0);
        assertThat(fila(reporte, "Distribuidora Sol").cantidadCompras()).isEqualTo(2);
        assertThat(fila(reporte, "Distribuidora Sol").totalVendido()).isEqualByComparingTo("125.00");
    }

    @Test
    @Transactional
    @DisplayName("Las filas salen ordenadas de mayor a menor total vendido")
    void ordenDeMayorAMenor() {
        cargarDatos();

        assertThat(reporte().filas()).extracting("cliente").containsExactly(
                "Consumidor final (mostrador)", "Distribuidora Sol", "Cliente viejo", "Ana Pérez");
    }

    @Test
    @Transactional
    @DisplayName("El genérico es su propia fila y suma al tipo Consumidor final")
    void generico_FilaPropiaYTipoUnificado() {
        cargarDatos();

        ReporteVentasPorClienteDTO reporte = reporte();
        VentasPorClienteFilaDTO mostrador = fila(reporte, "Consumidor final (mostrador)");

        assertThat(mostrador.tipo_cliente()).isEqualTo("CONSUMIDOR_FINAL");
        assertThat(mostrador.tipo()).isEqualTo("Consumidor final");
        assertThat(mostrador.cantidadCompras()).isEqualTo(1);
        assertThat(mostrador.totalVendido()).isEqualByComparingTo("200.00");

        assertThat(reporte.porTipo()).extracting("tipo")
                .containsExactly("Gran cliente", "Consumidor final");
        assertThat(porTipo(reporte, "CONSUMIDOR_FINAL").cantidadCompras()).isEqualTo(2);
        assertThat(porTipo(reporte, "CONSUMIDOR_FINAL").totalVendido())
                .isEqualByComparingTo("250.00");
        assertThat(porTipo(reporte, "GRAN_CLIENTE").cantidadCompras()).isEqualTo(3);
        assertThat(porTipo(reporte, "GRAN_CLIENTE").totalVendido())
                .isEqualByComparingTo("200.00");
    }

    @Test
    @Transactional
    @DisplayName("El filtro Consumidor final incluye al genérico y al registrado")
    void filtroConsumidorFinal_incluyeGenerico() {
        cargarDatos();

        ReporteVentasPorClienteDTO reporte = service.ventasPorCliente(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), null, "consumidor_final");

        assertThat(reporte.tipo_cliente()).isEqualTo("CONSUMIDOR_FINAL");
        assertThat(reporte.filas()).extracting("cliente")
                .containsExactly("Consumidor final (mostrador)", "Ana Pérez");
        assertThat(reporte.totales().cantidadCompras()).isEqualTo(2);
        assertThat(reporte.totales().totalVendido()).isEqualByComparingTo("250.00");
        assertThat(porTipo(reporte, "GRAN_CLIENTE").cantidadCompras()).isZero();
        assertThat(porTipo(reporte, "GRAN_CLIENTE").totalVendido())
                .isEqualByComparingTo("0.00");
    }

    @Test
    @Transactional
    @DisplayName("El filtro Gran cliente deja fuera a los consumidores finales")
    void filtroGranCliente() {
        cargarDatos();

        ReporteVentasPorClienteDTO reporte = service.ventasPorCliente(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), null, "GRAN_CLIENTE");

        assertThat(reporte.filas()).extracting("cliente")
                .containsExactly("Distribuidora Sol", "Cliente viejo");
        assertThat(reporte.totales().cantidadCompras()).isEqualTo(3);
        assertThat(reporte.totales().totalVendido()).isEqualByComparingTo("200.00");
    }

    @Test
    @Transactional
    @DisplayName("El filtro por cliente incluye a los inactivos y respeta el resumen")
    void filtroPorCliente_incluyeInactivos() {
        Clientes clientes = cargarDatos();

        ReporteVentasPorClienteDTO reporte = service.ventasPorCliente(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), clientes.inactivo(), null);

        assertThat(reporte.id_cliente()).isEqualTo(clientes.inactivo());
        assertThat(reporte.filas()).extracting("cliente").containsExactly("Cliente viejo");
        assertThat(fila(reporte, "Cliente viejo").totalVendido()).isEqualByComparingTo("75.00");
        assertThat(porTipo(reporte, "GRAN_CLIENTE").totalVendido())
                .isEqualByComparingTo("75.00");
        assertThat(porTipo(reporte, "CONSUMIDOR_FINAL").cantidadCompras()).isZero();
    }

    @Test
    @Transactional
    @DisplayName("Rango sin ventas: tabla vacía y los dos tipos del resumen en cero")
    void rangoSinVentas() {
        cargarDatos();

        ReporteVentasPorClienteDTO reporte = service.ventasPorCliente(
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 3), null, null);

        assertThat(reporte.filas()).isEmpty();
        assertThat(reporte.totales().cantidadCompras()).isZero();
        assertThat(reporte.totales().totalVendido()).isEqualByComparingTo("0.00");
        assertThat(reporte.porTipo()).hasSize(2);
        assertThat(reporte.porTipo()).allSatisfy(tipo -> {
            assertThat(tipo.cantidadCompras()).isZero();
            assertThat(tipo.totalVendido()).isEqualByComparingTo("0.00");
        });
    }

    @Test
    @DisplayName("Tipo de cliente desconocido: 400 de negocio")
    void tipoDesconocido() {
        assertThatThrownBy(() -> service.ventasPorCliente(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), null, "GENERICO"))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El tipo de cliente debe ser GRAN_CLIENTE o CONSUMIDOR_FINAL");
    }
}

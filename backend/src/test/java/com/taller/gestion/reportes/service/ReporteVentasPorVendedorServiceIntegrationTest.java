package com.taller.gestion.reportes.service;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.ReporteVentasPorVendedorDTO;
import com.taller.gestion.reportes.dto.VentasPorVendedorFilaDTO;
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
class ReporteVentasPorVendedorServiceIntegrationTest {

    private static final String ANA = "Ana Pérez (VENDEDOR, ana@taller.local)";
    private static final String ANA_NUEVA = "Ana Pérez (VENDEDOR, ana2@taller.local)";
    private static final String CARLOS = "Carlos Ruiz (VENDEDOR)";
    private static final String VIEJO = "Viejo Vendedor (VENDEDOR)";
    private static final String ADMIN = "María Directora (ADMINISTRADOR)";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ReporteVentasPorVendedorService service;

    private record Usuarios(long ana, long anaNueva, long carlos, long inactivo, long admin) {
    }

    private long guardarUsuario(long id, String nombre, String email, String rol, boolean activo) {
        jdbcTemplate.update(
                "INSERT INTO Usuario (id_usuario, nombre, email, password, rol, activo) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                id, nombre, email, "$2a$10$hashDePruebaNoUsadoParaLogin", rol, activo);
        return id;
    }

    private void guardarVenta(LocalDate fecha, String estado, String total, long idVendedor) {
        jdbcTemplate.update(
                "INSERT INTO Venta (id_cliente, id_vendedor, fecha, hora, total, estado) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                1L, idVendedor, Date.valueOf(fecha), Time.valueOf(LocalTime.of(10, 30)),
                new BigDecimal(total), estado);
    }

    /** El cliente 1 (genérico) ya existe al arrancar; acá solo importa el vendedor de la venta. */
    private Usuarios cargarDatos() {
        long ana = guardarUsuario(101L, "Ana Pérez", "ana@taller.local", "VENDEDOR", true);
        long anaNueva = guardarUsuario(105L, "Ana Pérez", "ana2@taller.local", "VENDEDOR", true);
        long carlos = guardarUsuario(102L, "Carlos Ruiz", "carlos@taller.local", "VENDEDOR", true);
        long inactivo = guardarUsuario(
                103L, "Viejo Vendedor", "viejo@taller.local", "VENDEDOR", false);
        long admin = guardarUsuario(
                104L, "María Directora", "maria@taller.local", "ADMINISTRADOR", true);

        guardarVenta(LocalDate.of(2026, 10, 1), "CONFIRMADA", "100.00", ana);
        guardarVenta(LocalDate.of(2026, 10, 2), "CONFIRMADA", "50.00", ana);
        guardarVenta(LocalDate.of(2026, 10, 2), "CONFIRMADA", "200.00", carlos);
        guardarVenta(LocalDate.of(2026, 10, 3), "CONFIRMADA", "75.00", inactivo);
        guardarVenta(LocalDate.of(2026, 10, 5), "CONFIRMADA", "125.00", admin);
        guardarVenta(LocalDate.of(2026, 10, 31), "CONFIRMADA", "25.00", anaNueva);
        // Ni la anulada ni la del mes siguiente pueden entrar en el reporte.
        guardarVenta(LocalDate.of(2026, 10, 4), "ANULADA", "999.00", ana);
        guardarVenta(LocalDate.of(2026, 11, 1), "CONFIRMADA", "500.00", ana);

        return new Usuarios(ana, anaNueva, carlos, inactivo, admin);
    }

    private ReporteVentasPorVendedorDTO reporte() {
        return service.ventasPorVendedor(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), null);
    }

    private VentasPorVendedorFilaDTO fila(ReporteVentasPorVendedorDTO reporte, String vendedor) {
        return reporte.filas().stream()
                .filter(f -> f.vendedor().equals(vendedor))
                .findFirst()
                .orElseThrow();
    }

    @Test
    @Transactional
    @DisplayName("Solo cuentan las ventas confirmadas del rango, inclusive el último día")
    void soloConfirmadasYUltimoDiaIncluido() {
        cargarDatos();

        ReporteVentasPorVendedorDTO reporte = reporte();

        assertThat(reporte.filas()).hasSize(5);
        assertThat(reporte.totales().cantidadVentas()).isEqualTo(6);
        assertThat(reporte.totales().totalVendido()).isEqualByComparingTo("575.00");
        assertThat(reporte.totales().ticketPromedio()).isEqualByComparingTo("95.83");
        assertThat(reporte.totales().vendedores()).isEqualTo(5);
        assertThat(reporte.filas()).noneMatch(
                f -> f.totalVendido().compareTo(new BigDecimal("999.00")) == 0);
        assertThat(reporte.filas()).noneMatch(
                f -> f.totalVendido().compareTo(new BigDecimal("500.00")) == 0);

        VentasPorVendedorFilaDTO ana = fila(reporte, ANA);
        assertThat(ana.cantidadVentas()).isEqualTo(2);
        assertThat(ana.totalVendido()).isEqualByComparingTo("150.00");
        assertThat(ana.ticketPromedio()).isEqualByComparingTo("75.00");
    }

    @Test
    @Transactional
    @DisplayName("Las filas salen ordenadas de mayor a menor total vendido")
    void ordenDeMayorAMenor() {
        cargarDatos();

        assertThat(reporte().filas()).extracting("vendedor").containsExactly(
                CARLOS, ANA, ADMIN, VIEJO, ANA_NUEVA);
    }

    @Test
    @Transactional
    @DisplayName("El nombre y el rol se muestran; nombres repetidos llevan también el email")
    void etiquetaConRolYEmailParaNombresRepetidos() {
        cargarDatos();

        ReporteVentasPorVendedorDTO reporte = reporte();

        assertThat(fila(reporte, CARLOS).vendedor()).isEqualTo("Carlos Ruiz (VENDEDOR)");
        assertThat(fila(reporte, ANA).vendedor()).contains("ana@taller.local");
        assertThat(fila(reporte, ANA_NUEVA).vendedor()).contains("ana2@taller.local");
        assertThat(reporte.filas())
                .noneMatch(f -> f.vendedor().contains("taller.local")
                        && !f.vendedor().equals(ANA) && !f.vendedor().equals(ANA_NUEVA));
    }

    @Test
    @Transactional
    @DisplayName("Un administrador con ventas aparece como una fila más")
    void incluyeAdministradorConVentas() {
        cargarDatos();

        VentasPorVendedorFilaDTO fila = fila(reporte(), ADMIN);

        assertThat(fila.cantidadVentas()).isEqualTo(1);
        assertThat(fila.totalVendido()).isEqualByComparingTo("125.00");
        assertThat(fila.ticketPromedio()).isEqualByComparingTo("125.00");
    }

    @Test
    @Transactional
    @DisplayName("El filtro por vendedor funciona también para un usuario inactivo")
    void filtroPorVendedor_incluyeInactivos() {
        Usuarios usuarios = cargarDatos();

        ReporteVentasPorVendedorDTO reporte = service.ventasPorVendedor(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), usuarios.inactivo());

        assertThat(reporte.id_vendedor()).isEqualTo(usuarios.inactivo());
        assertThat(reporte.filas()).extracting("vendedor").containsExactly(VIEJO);
        assertThat(fila(reporte, VIEJO).totalVendido()).isEqualByComparingTo("75.00");
        assertThat(reporte.totales().cantidadVentas()).isEqualTo(1);
    }

    @Test
    @Transactional
    @DisplayName("Los vendedores sin ventas confirmadas en el rango no aparecen")
    void sinVentasEnElRango_tablaVaciaYTotalesEnCero() {
        cargarDatos();

        ReporteVentasPorVendedorDTO reporte = service.ventasPorVendedor(
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 3), null);

        assertThat(reporte.filas()).isEmpty();
        assertThat(reporte.totales().cantidadVentas()).isZero();
        assertThat(reporte.totales().totalVendido()).isEqualByComparingTo("0.00");
        assertThat(reporte.totales().ticketPromedio()).isEqualByComparingTo("0.00");
        assertThat(reporte.totales().vendedores()).isZero();
    }

    @Test
    @DisplayName("Rango inválido: 400 de negocio")
    void rangoInvalido() {
        assertThatThrownBy(() -> service.ventasPorVendedor(null, LocalDate.of(2026, 10, 31), null))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El rango de fechas es obligatorio");
        assertThatThrownBy(() -> service.ventasPorVendedor(
                LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 1), null))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("La fecha 'desde' no puede ser posterior a 'hasta'");
    }
}

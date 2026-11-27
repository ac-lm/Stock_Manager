package com.taller.gestion.reportes.service;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.ComparacionEstacionalFilaDTO;
import com.taller.gestion.reportes.dto.ReporteComparacionEstacionalDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ReporteComparacionEstacionalServiceIntegrationTest.RelojFijo.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
class ReporteComparacionEstacionalServiceIntegrationTest {

    private static final String ANIOS = "2024,2025,2026";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ReporteComparacionEstacionalService service;

    static class RelojFijo {
        @Bean
        @Primary
        Clock relojFijoParaTests() {
            return Clock.fixed(Instant.parse("2026-11-26T22:45:00Z"), ZoneOffset.UTC);
        }
    }

    private record Productos(long cafe, long papel) {
    }

    private long guardarProducto(long id, String nombre, String categoria) {
        jdbcTemplate.update(
                "INSERT INTO Producto (id_producto, nombre, precio, stock_actual, categoria) "
                        + "VALUES (?, ?, ?, ?, ?)",
                id, nombre, new BigDecimal("10.00"), new BigDecimal("5.000"), categoria);
        return id;
    }

    private long guardarVenta(LocalDate fecha, String estado, String total) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO Venta (id_cliente, id_vendedor, fecha, hora, total, estado) "
                        + "VALUES (?, ?, ?, ?, ?, ?) RETURNING id_venta",
                Long.class,
                1L, 5L, Date.valueOf(fecha), Time.valueOf(LocalTime.of(10, 30)),
                new BigDecimal(total), estado);
    }

    private void guardarDetalle(long idVenta, long idProducto, String subtotal) {
        jdbcTemplate.update(
                "INSERT INTO Detalle_Venta (id_venta_fk, id_producto_fk, cantidad, "
                        + "precio_historico, subtotal) VALUES (?, ?, ?, ?, ?)",
                idVenta, idProducto, new BigDecimal("1.000"),
                new BigDecimal(subtotal), new BigDecimal(subtotal));
    }

    private Productos cargarDatos() {
        long cafe = guardarProducto(101L, "Café", "Bebidas");
        long papel = guardarProducto(102L, "Papel", "Papelería");

        long venta2024Ene = guardarVenta(LocalDate.of(2024, 1, 15), "CONFIRMADA", "100.00");
        guardarDetalle(venta2024Ene, cafe, "100.00");

        long venta2024Mar = guardarVenta(LocalDate.of(2024, 3, 10), "CONFIRMADA", "200.00");
        guardarDetalle(venta2024Mar, cafe, "120.00");
        guardarDetalle(venta2024Mar, papel, "80.00");
        guardarVenta(LocalDate.of(2024, 3, 12), "ANULADA", "999.00");

        long venta2025Ene = guardarVenta(LocalDate.of(2025, 1, 20), "CONFIRMADA", "150.00");
        guardarDetalle(venta2025Ene, cafe, "150.00");

        long venta2025Mar = guardarVenta(LocalDate.of(2025, 3, 5), "CONFIRMADA", "100.00");
        guardarDetalle(venta2025Mar, papel, "100.00");

        long venta2026Ene = guardarVenta(LocalDate.of(2026, 1, 7), "CONFIRMADA", "300.00");
        guardarDetalle(venta2026Ene, cafe, "200.00");
        guardarDetalle(venta2026Ene, papel, "100.00");

        long venta2026Mar = guardarVenta(LocalDate.of(2026, 3, 9), "CONFIRMADA", "50.00");
        guardarDetalle(venta2026Mar, cafe, "50.00");

        long ventaFutura = guardarVenta(LocalDate.of(2026, 12, 10), "CONFIRMADA", "777.00");
        guardarDetalle(ventaFutura, cafe, "777.00");

        return new Productos(cafe, papel);
    }

    private ComparacionEstacionalFilaDTO fila(ReporteComparacionEstacionalDTO reporte, int mes) {
        return reporte.filas().stream()
                .filter(f -> f.mes() == mes)
                .findFirst()
                .orElseThrow();
    }

    @Test
    @Transactional
    @DisplayName("Une por mes y año, con 0 en los meses vacíos y sin meses futuros")
    void mesesYAnios() {
        cargarDatos();

        ReporteComparacionEstacionalDTO reporte =
                service.comparacionEstacional(ANIOS, null, null);

        assertThat(reporte.anios()).containsExactly(2024, 2025, 2026);
        assertThat(reporte.desde()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(reporte.hasta()).isEqualTo(LocalDate.of(2026, 11, 30));
        assertThat(reporte.filas()).hasSize(11);
        assertThat(reporte.filas()).extracting("etiqueta").startsWith("ene", "feb", "mar");
        assertThat(reporte.filas()).noneMatch(f -> f.mes() == 12);

        ComparacionEstacionalFilaDTO ene = fila(reporte, 1);
        assertThat(ene.totalesPorAnio())
                .containsEntry("2024", new BigDecimal("100.00"))
                .containsEntry("2025", new BigDecimal("150.00"))
                .containsEntry("2026", new BigDecimal("300.00"));
        assertThat(ene.variacionesPorAnio().get("2025")).isEqualByComparingTo("50.00");
        assertThat(ene.variacionesPorAnio().get("2026")).isEqualByComparingTo("100.00");

        ComparacionEstacionalFilaDTO mar = fila(reporte, 3);
        assertThat(mar.totalesPorAnio())
                .containsEntry("2024", new BigDecimal("200.00"))
                .containsEntry("2025", new BigDecimal("100.00"))
                .containsEntry("2026", new BigDecimal("50.00"));
        assertThat(mar.variacionesPorAnio().get("2025")).isEqualByComparingTo("-50.00");
        assertThat(mar.variacionesPorAnio().get("2026")).isEqualByComparingTo("-50.00");

        ComparacionEstacionalFilaDTO abr = fila(reporte, 4);
        assertThat(abr.totalesPorAnio().values()).allMatch(t -> t.compareTo(BigDecimal.ZERO) == 0);
        assertThat(abr.variacionesPorAnio()).isEmpty();

        assertThat(reporte.totales().totalesPorAnio())
                .containsEntry("2024", new BigDecimal("300.00"))
                .containsEntry("2025", new BigDecimal("250.00"))
                .containsEntry("2026", new BigDecimal("350.00"));
        assertThat(reporte.totales().variacionesPorAnio().get("2025"))
                .isEqualByComparingTo("-16.67");
        assertThat(reporte.totales().variacionesPorAnio().get("2026"))
                .isEqualByComparingTo("40.00");
        assertThat(reporte.filas()).noneMatch(
                f -> f.totalesPorAnio().containsValue(new BigDecimal("777.00")));
        assertThat(reporte.filas()).noneMatch(
                f -> f.totalesPorAnio().containsValue(new BigDecimal("999.00")));
    }

    @Test
    @Transactional
    @DisplayName("Sin filtro suma Venta.total; con producto o categoría suma las líneas")
    void filtrosDeProductoYCategoria() {
        Productos productos = cargarDatos();

        ReporteComparacionEstacionalDTO porProducto =
                service.comparacionEstacional(ANIOS, productos.cafe(), null);
        assertThat(fila(porProducto, 1).totalesPorAnio())
                .containsEntry("2024", new BigDecimal("100.00"))
                .containsEntry("2025", new BigDecimal("150.00"))
                .containsEntry("2026", new BigDecimal("200.00"));
        assertThat(fila(porProducto, 3).totalesPorAnio())
                .containsEntry("2024", new BigDecimal("120.00"))
                .containsEntry("2025", new BigDecimal("0.00"))
                .containsEntry("2026", new BigDecimal("50.00"));
        assertThat(porProducto.totales().totalesPorAnio())
                .containsEntry("2024", new BigDecimal("220.00"))
                .containsEntry("2025", new BigDecimal("150.00"))
                .containsEntry("2026", new BigDecimal("250.00"));

        ReporteComparacionEstacionalDTO porCategoria =
                service.comparacionEstacional(ANIOS, null, "Bebidas");
        assertThat(porCategoria.totales().totalesPorAnio())
                .isEqualTo(porProducto.totales().totalesPorAnio());

        ReporteComparacionEstacionalDTO papeleria =
                service.comparacionEstacional(ANIOS, null, "Papelería");
        assertThat(fila(papeleria, 3).totalesPorAnio())
                .containsEntry("2024", new BigDecimal("80.00"))
                .containsEntry("2025", new BigDecimal("100.00"))
                .containsEntry("2026", new BigDecimal("0.00"));
    }

    @Test
    @Transactional
    @DisplayName("Sin años: usa el actual y los dos anteriores, según el reloj del taller")
    void aniosPorDefecto() {
        assertThat(service.comparacionEstacional(null, null, null).anios())
                .containsExactly(2024, 2025, 2026);
        assertThat(service.comparacionEstacional("  ", null, null).anios())
                .containsExactly(2024, 2025, 2026);
    }

    @Test
    @DisplayName("Años inválidos: 400 de negocio")
    void aniosInvalidos() {
        assertThatThrownBy(() -> service.comparacionEstacional("2024,abc", null, null))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El parámetro 'anios' tiene un formato inválido");
        assertThatThrownBy(() -> service.comparacionEstacional("2024,2024", null, null))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("Los años a comparar no se pueden repetir");
        assertThatThrownBy(() -> service.comparacionEstacional(
                "2019,2020,2021,2022,2023,2024", null, null))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("Se pueden comparar hasta 5 años");
        assertThatThrownBy(() -> service.comparacionEstacional("1999", null, null))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("Los años deben estar entre 2000 y 2026");
        assertThatThrownBy(() -> service.comparacionEstacional("2027", null, null))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("Los años deben estar entre 2000 y 2026");
    }
}

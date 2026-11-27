package com.taller.gestion.reportes.service;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.ReporteStockActualDTO;
import com.taller.gestion.reportes.dto.StockActualFilaDTO;
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
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ReporteStockActualServiceIntegrationTest.RelojFijo.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
class ReporteStockActualServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ReporteStockActualService service;

    /** 02:00 UTC del 27 ya es 23:00 del 26 en el taller: la foto debe ser el 26. */
    static class RelojFijo {
        @Bean
        @Primary
        Clock relojFijoParaTests() {
            return Clock.fixed(Instant.parse("2026-11-27T02:00:00Z"), ZoneOffset.UTC);
        }
    }

    private void guardarProducto(long id, String nombre, String categoria, String stock) {
        jdbcTemplate.update(
                "INSERT INTO Producto (id_producto, nombre, precio, stock_actual, categoria) "
                        + "VALUES (?, ?, ?, ?, ?)",
                id, nombre, new BigDecimal("100.00"), new BigDecimal(stock), categoria);
    }

    private void guardarInsumo(long id, String nombre, String unidad, String stock, int minimo) {
        jdbcTemplate.update(
                "INSERT INTO Insumo (id_insumo, nombre, stock_actual, unidad_medida, "
                        + "stock_minimo, costo) VALUES (?, ?, ?, ?, ?, ?)",
                id, nombre, new BigDecimal(stock), unidad, minimo, new BigDecimal("10.00"));
    }

    private void cargarDatos() {
        guardarProducto(1L, "Café", "Bebidas", "2.000");
        guardarProducto(2L, "Mate", "Bebidas", "0.000");
        guardarInsumo(10L, "Harina", "kg", "2.000", 5);
        guardarInsumo(11L, "Azúcar", "kg", "8.000", 5);
        guardarInsumo(12L, "Leche", "lt", "1.500", 1);
    }

    private StockActualFilaDTO fila(ReporteStockActualDTO reporte, String articulo) {
        return reporte.filas().stream()
                .filter(f -> f.articulo().equals(articulo))
                .findFirst()
                .orElseThrow();
    }

    @Test
    @Transactional
    @DisplayName("La foto es de hoy en la zona del taller, sin rango de fechas")
    void fotoDeHoy_enZonaDelTaller() {
        cargarDatos();

        ReporteStockActualDTO reporte = service.stockActual(null, null, false);

        assertThat(reporte.desde()).isEqualTo(LocalDate.of(2026, 11, 26));
        assertThat(reporte.hasta()).isEqualTo(reporte.desde());
        assertThat(reporte.tipo()).isEqualTo("TODOS");
        assertThat(reporte.categoria()).isNull();
        assertThat(reporte.soloBajoMinimo()).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("Lee stock_actual, muestra N/A en productos y calcula la falta en insumos")
    void filasConStockMinimoYFalta() {
        cargarDatos();

        ReporteStockActualDTO reporte = service.stockActual(null, null, false);

        assertThat(reporte.filas()).hasSize(5);
        // ORDER BY tipo, nombre: INSUMO va antes que PRODUCTO por orden alfabético.
        assertThat(reporte.filas()).extracting(StockActualFilaDTO::articulo)
                .containsExactly("Azúcar", "Harina", "Leche", "Café", "Mate");

        StockActualFilaDTO cafe = fila(reporte, "Café");
        assertThat(cafe.tipo().name()).isEqualTo("PRODUCTO");
        assertThat(cafe.categoria()).isEqualTo("Bebidas");
        assertThat(cafe.unidad()).isNull();
        assertThat(cafe.stockActual()).isEqualByComparingTo("2.000");
        assertThat(cafe.stockMinimo()).isNull();
        assertThat(cafe.falta()).isNull();
        assertThat(cafe.bajoMinimo()).isFalse();

        StockActualFilaDTO mate = fila(reporte, "Mate");
        assertThat(mate.stockActual()).isEqualByComparingTo("0.000");
        assertThat(mate.bajoMinimo()).isFalse();

        StockActualFilaDTO harina = fila(reporte, "Harina");
        assertThat(harina.tipo().name()).isEqualTo("INSUMO");
        assertThat(harina.categoria()).isNull();
        assertThat(harina.unidad()).isEqualTo("kg");
        assertThat(harina.stockMinimo()).isEqualTo(5);
        assertThat(harina.falta()).isEqualByComparingTo("3.000");
        assertThat(harina.bajoMinimo()).isTrue();

        StockActualFilaDTO leche = fila(reporte, "Leche");
        assertThat(leche.stockActual()).isEqualByComparingTo("1.500");
        assertThat(leche.stockMinimo()).isEqualTo(1);
        assertThat(leche.falta()).isEqualByComparingTo("0.000");
        assertThat(leche.bajoMinimo()).isFalse();

        assertThat(reporte.totales().articulos()).isEqualTo(5);
        assertThat(reporte.totales().bajoMinimo()).isEqualTo(1);
    }

    @Test
    @Transactional
    @DisplayName("El gráfico solo incluye bajo mínimo, ordenados por mayor falta")
    void graficoSoloBajoMinimoOrdenadoPorFalta() {
        guardarProducto(1L, "Café", "Bebidas", "2.000");
        guardarInsumo(10L, "Harina", "kg", "2.000", 5);   // falta 3
        guardarInsumo(11L, "Azúcar", "kg", "1.000", 4);   // falta 3, desempata por nombre
        guardarInsumo(12L, "Leche", "lt", "0.500", 3);    // falta 2,5

        ReporteStockActualDTO reporte = service.stockActual(null, null, false);

        assertThat(reporte.bajoMinimo()).extracting(StockActualFilaDTO::articulo)
                .containsExactly("Azúcar", "Harina", "Leche");
        assertThat(reporte.bajoMinimo()).extracting(StockActualFilaDTO::falta)
                .extracting(BigDecimal::stripTrailingZeros)
                .containsExactly(new BigDecimal("3"), new BigDecimal("3"), new BigDecimal("2.5"));
    }

    @Test
    @Transactional
    @DisplayName("Solo bajo mínimo acota la tabla sin sacar artículos del gráfico")
    void filtroSoloBajoMinimo() {
        cargarDatos();

        ReporteStockActualDTO reporte = service.stockActual(null, null, true);

        assertThat(reporte.soloBajoMinimo()).isTrue();
        assertThat(reporte.filas()).extracting(StockActualFilaDTO::articulo).containsExactly("Harina");
        assertThat(reporte.bajoMinimo()).extracting(StockActualFilaDTO::articulo).containsExactly("Harina");
        assertThat(reporte.totales().articulos()).isEqualTo(1);
        assertThat(reporte.totales().bajoMinimo()).isEqualTo(1);
    }

    @Test
    @Transactional
    @DisplayName("El filtro de tipo elige una sola clase de artículo")
    void filtroPorTipo() {
        cargarDatos();

        ReporteStockActualDTO insumos = service.stockActual("INSUMO", null, false);
        assertThat(insumos.filas()).hasSize(3);
        assertThat(insumos.filas()).allSatisfy(f -> assertThat(f.tipo().name()).isEqualTo("INSUMO"));

        ReporteStockActualDTO productos = service.stockActual("producto", null, false);
        assertThat(productos.filas()).hasSize(2);
        assertThat(productos.tipo()).isEqualTo("PRODUCTO");
    }

    @Test
    @Transactional
    @DisplayName("Los insumos no tienen categoría: al filtrar por categoría se ocultan")
    void filtroPorCategoria_excluyeInsumos() {
        cargarDatos();

        ReporteStockActualDTO reporte = service.stockActual(null, "Bebidas", false);

        assertThat(reporte.categoria()).isEqualTo("Bebidas");
        assertThat(reporte.filas()).extracting(StockActualFilaDTO::articulo).containsExactly("Café", "Mate");
        assertThat(reporte.filas()).allSatisfy(f -> assertThat(f.tipo().name()).isEqualTo("PRODUCTO"));

        assertThat(service.stockActual(null, "Inexistente", false).filas()).isEmpty();
    }

    @Test
    @Transactional
    @DisplayName("Un tipo inválido se responde como error de datos")
    void tipoInvalido_lanzaDatosInvalidos() {
        assertThatThrownBy(() -> service.stockActual("BEBIDA", null, false))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El parámetro 'tipo' tiene un valor inválido");
    }
}

package com.taller.gestion.reportes.exportacion;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.taller.gestion.reportes.dto.ReporteVentasPorPeriodoDTO;
import com.taller.gestion.reportes.service.ReporteVentasService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureMockMvc
@Import(ReporteExportacionIntegrationTest.RelojFijo.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
@WithMockUser(roles = "ADMINISTRADOR")
class ReporteExportacionIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ReporteVentasService reporteVentasService;

    @Autowired
    private FormateadorValoresExportacion formateadorValores;

    static class RelojFijo {
        @Bean
        @Primary
        Clock relojFijoParaTests() {
            return Clock.fixed(Instant.parse("2026-11-26T22:45:00Z"), ZoneOffset.UTC);
        }
    }

    private long guardarVenta(LocalDate fecha, String estado, String total) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO Venta (id_cliente, id_vendedor, fecha, hora, total, estado) "
                        + "VALUES (?, ?, ?, ?, ?, ?) RETURNING id_venta",
                Long.class,
                2L, 5L, Date.valueOf(fecha), Time.valueOf(LocalTime.of(10, 30)),
                new BigDecimal(total), estado);
    }

    @Test
    @DisplayName("Los tres archivos descargados coinciden con lo que muestra la pantalla")
    void exportar_coincideConReporteDePantalla() throws Exception {
        guardarVenta(LocalDate.of(2026, 10, 1), "CONFIRMADA", "100.00");
        guardarVenta(LocalDate.of(2026, 10, 1), "CONFIRMADA", "50.00");
        guardarVenta(LocalDate.of(2026, 10, 2), "ANULADA", "999.00");
        guardarVenta(LocalDate.of(2026, 10, 3), "CONFIRMADA", "200.00");

        ReporteVentasPorPeriodoDTO pantalla = reporteVentasService.ventasPorPeriodo(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3), "dia");
        assertThat(pantalla.totales().totalVendido()).isEqualByComparingTo("350.00");
        assertThat(pantalla.filas()).hasSize(3);

        List<String[]> formatos = List.of(
                new String[]{"xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"},
                new String[]{"csv", "text/csv;charset=UTF-8"},
                new String[]{"pdf", "application/pdf"});

        for (String[] formato : formatos) {
            String extension = formato[0];
            MvcResult resultado = mockMvc.perform(get("/api/reportes/exportar")
                            .param("reporte", "ventas-por-periodo")
                            .param("formato", extension)
                            .param("desde", "2026-10-01")
                            .param("hasta", "2026-10-03")
                            .param("agrupacion", "dia"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, formato[1]))
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"ventas-por-periodo_2026-10-01_2026-10-03."
                                    + extension + "\""))
                    .andReturn();

            byte[] contenido = resultado.getResponse().getContentAsByteArray();
            assertThat(contenido).describedAs("archivo " + extension).isNotEmpty();

            String texto = textoExportado(extension, contenido);
            assertThat(texto).describedAs("contenido " + extension)
                    .contains("Ventas por período")
                    .contains("Desde: 2026-10-01")
                    .contains("Hasta: 2026-10-03")
                    .contains("Agrupado por día")
                    .contains("Emitido: 26/11/2026 22:45")
                    .doesNotContain("ART")
                    .contains("Totales");

            for (var fila : pantalla.filas()) {
                assertThat(texto).describedAs("fila " + fila.etiqueta() + " en " + extension)
                        .contains(fila.etiqueta());
            }
            assertThat(texto).describedAs("total en " + extension).contains(
                    formateadorValores.formatear(
                            pantalla.totales().totalVendido(), TipoDatoExportable.DECIMAL));
        }
    }

    @Test
    @Transactional
    @DisplayName("REP-03: los tres archivos usan la misma consulta y los filtros del reporte")
    void exportarVentasPorProducto() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO Producto (id_producto, nombre, precio, stock_actual, categoria) "
                        + "VALUES (?, ?, ?, ?, ?)",
                1L, "Café en grano", new BigDecimal("999.99"), new BigDecimal("10.000"), "Bebidas");
        long venta = guardarVenta(LocalDate.of(2026, 10, 1), "CONFIRMADA", "150.00");
        jdbcTemplate.update(
                "INSERT INTO Detalle_Venta (id_venta_fk, id_producto_fk, cantidad, "
                        + "precio_historico, subtotal) VALUES (?, ?, ?, ?, ?)",
                venta, 1L, new BigDecimal("3.000"), new BigDecimal("999.99"),
                new BigDecimal("150.00"));

        for (String[] formato : List.of(
                new String[]{"xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"},
                new String[]{"csv", "text/csv;charset=UTF-8"},
                new String[]{"pdf", "application/pdf"})) {
            String extension = formato[0];
            MvcResult resultado = mockMvc.perform(get("/api/reportes/exportar")
                            .param("reporte", "ventas-por-producto")
                            .param("formato", extension)
                            .param("desde", "2026-10-01")
                            .param("hasta", "2026-10-03")
                            .param("categoria", "Bebidas")
                            .param("top", "5"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, formato[1]))
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"ventas-por-producto_2026-10-01_2026-10-03."
                                    + extension + "\""))
                    .andReturn();

            String texto = textoExportado(extension, resultado.getResponse().getContentAsByteArray());
            assertThat(texto).describedAs("contenido " + extension)
                    .contains("Ventas por producto")
                    .contains("Desde: 2026-10-01")
                    .contains("Hasta: 2026-10-03")
                    .contains("Categoría: Bebidas")
                    .contains("Top 5 por total vendido")
                    .contains("Emitido: 26/11/2026 22:45")
                    .contains("Producto")
                    .contains("Cantidad vendida")
                    .contains("Café en grano")
                    .contains(formateadorValores.formatear(
                            new BigDecimal("150.00"), TipoDatoExportable.DECIMAL))
                    .contains("Totales");
        }
    }

    @Test
    @Transactional
    @DisplayName("REP-04: la exportación usa la tabla por cliente y los filtros de la pantalla")
    void exportarVentasPorCliente() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO Cliente (id_cliente, CUIT, nombre, telefono, email, direccion, "
                        + "tipo_cliente, observaciones, activo, plazo_pago_dias, nombre_normalizado) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                101L, null, "Ana Pérez", null, "ana@taller.local", "Calle 1",
                "CONSUMIDOR_FINAL", null, true, null, "ana perez");
        jdbcTemplate.update(
                "INSERT INTO Cliente (id_cliente, CUIT, nombre, telefono, email, direccion, "
                        + "tipo_cliente, observaciones, activo, plazo_pago_dias, nombre_normalizado) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                102L, null, "Distribuidora Sol", null, "sol@taller.local", "Calle 2",
                "GRAN_CLIENTE", null, true, null, "distribuidora sol");
        long mostrador = jdbcTemplate.queryForObject(
                "SELECT id_cliente FROM Cliente WHERE tipo_cliente = 'GENERICO' "
                        + "ORDER BY id_cliente LIMIT 1", Long.class);

        jdbcTemplate.update(
                "INSERT INTO Venta (id_cliente, id_vendedor, fecha, hora, total, estado) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                101L, 5L, Date.valueOf(LocalDate.of(2026, 10, 2)),
                Time.valueOf(LocalTime.of(10, 30)), new BigDecimal("50.00"), "CONFIRMADA");
        jdbcTemplate.update(
                "INSERT INTO Venta (id_cliente, id_vendedor, fecha, hora, total, estado) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                mostrador, 5L, Date.valueOf(LocalDate.of(2026, 10, 3)),
                Time.valueOf(LocalTime.of(10, 30)), new BigDecimal("200.00"), "CONFIRMADA");
        jdbcTemplate.update(
                "INSERT INTO Venta (id_cliente, id_vendedor, fecha, hora, total, estado) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                102L, 5L, Date.valueOf(LocalDate.of(2026, 10, 4)),
                Time.valueOf(LocalTime.of(10, 30)), new BigDecimal("999.00"), "ANULADA");

        for (String[] formato : List.of(
                new String[]{"xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"},
                new String[]{"csv", "text/csv;charset=UTF-8"},
                new String[]{"pdf", "application/pdf"})) {
            String extension = formato[0];
            MvcResult resultado = mockMvc.perform(get("/api/reportes/exportar")
                            .param("reporte", "ventas-por-cliente")
                            .param("formato", extension)
                            .param("desde", "2026-10-01")
                            .param("hasta", "2026-10-31")
                            .param("tipo_cliente", "CONSUMIDOR_FINAL"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, formato[1]))
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"ventas-por-cliente_2026-10-01_2026-10-31."
                                    + extension + "\""))
                    .andReturn();

            String texto = textoExportado(extension, resultado.getResponse().getContentAsByteArray());
            assertThat(texto).describedAs("contenido " + extension)
                    .contains("Ventas por cliente")
                    .contains("Desde: 2026-10-01")
                    .contains("Hasta: 2026-10-31")
                    .contains("Tipo de cliente: Consumidor final")
                    .contains("Emitido: 26/11/2026 22:45")
                    .contains("Cliente")
                    .contains("Cantidad de compras")
                    .contains("Ana Pérez")
                    .contains("Consumidor final (mostrador)")
                    .contains(formateadorValores.formatear(
                            new BigDecimal("200.00"), TipoDatoExportable.DECIMAL))
                    .contains("Totales")
                    .doesNotContain("Distribuidora Sol");
        }
    }

    @Test
    @Transactional
    @DisplayName("REP-05: la exportación usa la tabla por vendedor y los filtros de la pantalla")
    void exportarVentasPorVendedor() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO Usuario (id_usuario, nombre, email, password, rol, activo) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                101L, "Ana Pérez", "ana@taller.local", "$2a$10$hashDePrueba", "VENDEDOR", true);
        jdbcTemplate.update(
                "INSERT INTO Venta (id_cliente, id_vendedor, fecha, hora, total, estado) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                1L, 101L, Date.valueOf(LocalDate.of(2026, 10, 2)),
                Time.valueOf(LocalTime.of(10, 30)), new BigDecimal("50.00"), "CONFIRMADA");
        jdbcTemplate.update(
                "INSERT INTO Venta (id_cliente, id_vendedor, fecha, hora, total, estado) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                1L, 101L, Date.valueOf(LocalDate.of(2026, 10, 3)),
                Time.valueOf(LocalTime.of(10, 30)), new BigDecimal("200.00"), "CONFIRMADA");
        jdbcTemplate.update(
                "INSERT INTO Venta (id_cliente, id_vendedor, fecha, hora, total, estado) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                1L, 101L, Date.valueOf(LocalDate.of(2026, 10, 4)),
                Time.valueOf(LocalTime.of(10, 30)), new BigDecimal("999.00"), "ANULADA");

        for (String[] formato : List.of(
                new String[]{"xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"},
                new String[]{"csv", "text/csv;charset=UTF-8"},
                new String[]{"pdf", "application/pdf"})) {
            String extension = formato[0];
            MvcResult resultado = mockMvc.perform(get("/api/reportes/exportar")
                            .param("reporte", "ventas-por-vendedor")
                            .param("formato", extension)
                            .param("desde", "2026-10-01")
                            .param("hasta", "2026-10-31")
                            .param("id_vendedor", "101"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, formato[1]))
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"ventas-por-vendedor_2026-10-01_2026-10-31."
                                    + extension + "\""))
                    .andReturn();

            String texto = textoExportado(extension, resultado.getResponse().getContentAsByteArray());
            assertThat(texto).describedAs("contenido " + extension)
                    .contains("Ventas por vendedor")
                    .contains("Desde: 2026-10-01")
                    .contains("Hasta: 2026-10-31")
                    .contains("Vendedor: 101")
                    .contains("Emitido: 26/11/2026 22:45")
                    .contains("Cantidad de ventas")
                    .contains("Ticket promedio")
                    .contains("Ana Pérez (VENDEDOR)")
                    .contains(formateadorValores.formatear(
                            new BigDecimal("250.00"), TipoDatoExportable.DECIMAL))
                    .contains(formateadorValores.formatear(
                            new BigDecimal("125.00"), TipoDatoExportable.DECIMAL))
                    .contains("Totales")
                    .doesNotContain(formateadorValores.formatear(
                            new BigDecimal("999.00"), TipoDatoExportable.DECIMAL));
        }
    }

    @Test
    @Transactional
    @DisplayName("REP-06: los tres archivos usan la comparación por año y los filtros de la pantalla")
    void exportarComparacionEstacional() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO Producto (id_producto, nombre, precio, stock_actual, categoria) "
                        + "VALUES (?, ?, ?, ?, ?)",
                101L, "Café en grano", new BigDecimal("10.00"), new BigDecimal("5.000"), "Bebidas");
        long venta2025 = guardarVenta(LocalDate.of(2025, 1, 10), "CONFIRMADA", "100.00");
        jdbcTemplate.update(
                "INSERT INTO Detalle_Venta (id_venta_fk, id_producto_fk, cantidad, "
                        + "precio_historico, subtotal) VALUES (?, ?, ?, ?, ?)",
                venta2025, 101L, new BigDecimal("1.000"),
                new BigDecimal("100.00"), new BigDecimal("100.00"));
        long venta2026 = guardarVenta(LocalDate.of(2026, 1, 10), "CONFIRMADA", "200.00");
        jdbcTemplate.update(
                "INSERT INTO Detalle_Venta (id_venta_fk, id_producto_fk, cantidad, "
                        + "precio_historico, subtotal) VALUES (?, ?, ?, ?, ?)",
                venta2026, 101L, new BigDecimal("1.000"),
                new BigDecimal("200.00"), new BigDecimal("200.00"));

        for (String[] formato : List.of(
                new String[]{"xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"},
                new String[]{"csv", "text/csv;charset=UTF-8"},
                new String[]{"pdf", "application/pdf"})) {
            String extension = formato[0];
            MvcResult resultado = mockMvc.perform(get("/api/reportes/exportar")
                            .param("reporte", "evolucion-ventas")
                            .param("formato", extension)
                            .param("anios", "2024,2025,2026")
                            .param("id_producto", "101")
                            .param("categoria", "Bebidas"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, formato[1]))
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"evolucion-ventas_2024-01-01_2026-11-30."
                                    + extension + "\""))
                    .andReturn();

            String texto = textoExportado(extension, resultado.getResponse().getContentAsByteArray());
            assertThat(texto).describedAs("contenido " + extension)
                    .contains("Evolución de ventas y comparación estacional")
                    .contains("Años: 2024, 2025, 2026")
                    .contains("Producto: 101")
                    .contains("Categoría: Bebidas")
                    .contains("Emitido: 26/11/2026 22:45")
                    .contains("Mes")
                    .contains("Var.")
                    .contains("ene")
                    .contains("Totales")
                    .contains(formateadorValores.formatear(
                            new BigDecimal("100.00"), TipoDatoExportable.DECIMAL))
                    .contains(formateadorValores.formatear(
                            new BigDecimal("200.00"), TipoDatoExportable.DECIMAL))
                    .contains("100,00 %");
        }
    }

    @Test
    @DisplayName("REP-07: la descarga de cierres coincide con el reporte de pantalla")
    void exportarCierres_coincideConReporteDePantalla() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO Usuario (id_usuario, nombre, email, password, rol, activo) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                301L, "Ana Abre", "anaCierres@taller.local", "hash", "VENDEDOR", true);
        jdbcTemplate.update(
                "INSERT INTO Usuario (id_usuario, nombre, email, password, rol, activo) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                302L, "Luis Cierra", "luisCierres@taller.local", "hash", "VENDEDOR", true);
        long sesion = jdbcTemplate.queryForObject(
                "INSERT INTO Sesion_Caja (id_usuario_apertura, id_usuario_cierre, fecha_apertura, "
                        + "hora_apertura, fecha_cierre, hora_cierre, monto_inicial, estado) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id_sesion",
                Long.class, 301L, 302L, Date.valueOf(LocalDate.of(2026, 10, 1)),
                Time.valueOf(LocalTime.of(9, 0)), Date.valueOf(LocalDate.of(2026, 10, 1)),
                Time.valueOf(LocalTime.of(17, 0)), new BigDecimal("100.00"), "CERRADA");
        jdbcTemplate.update(
                "INSERT INTO Cierre_Caja_Detalle (id_sesion, medio_pago, total_esperado, "
                        + "total_declarado, diferencia) VALUES (?, ?, ?, ?, ?)",
                sesion, "EFECTIVO", new BigDecimal("100.00"), new BigDecimal("90.00"),
                new BigDecimal("-10.00"));
        jdbcTemplate.update(
                "INSERT INTO Movimiento_Caja (id_sesion_caja, id_usuario, tipo, concepto, monto, "
                        + "medio_pago, fecha, hora) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                sesion, 301L, "INGRESO", "VENTA", new BigDecimal("100.00"), "EFECTIVO",
                Date.valueOf(LocalDate.of(2026, 10, 1)), Time.valueOf(LocalTime.of(10, 0)));

        List<String[]> formatos = List.of(
                new String[]{"xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"},
                new String[]{"csv", "text/csv;charset=UTF-8"},
                new String[]{"pdf", "application/pdf"});

        for (String[] formato : formatos) {
            String extension = formato[0];
            MvcResult resultado = mockMvc.perform(get("/api/reportes/exportar")
                            .param("reporte", "cierres-caja")
                            .param("formato", extension)
                            .param("desde", "2026-10-01")
                            .param("hasta", "2026-10-03"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, formato[1]))
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"cierres-caja_2026-10-01_2026-10-03."
                                    + extension + "\""))
                    .andReturn();

            String texto = textoExportado(extension, resultado.getResponse().getContentAsByteArray());
            assertThat(texto).describedAs("contenido " + extension)
                    .contains("Reporte de cierres de caja")
                    .contains("Desde: 2026-10-01")
                    .contains("Hasta: 2026-10-03")
                    .contains("Estado: Todas (cerradas y pendientes de revisión)")
                    .contains("Medio de pago: Todos")
                    .contains("Emitido: 26/11/2026 22:45")
                    // El PDF parte las celdas angostas: se verifican fragmentos estables.
                    .contains("EFEC")
                    .contains("CERR")
                    .contains(formateadorValores.formatear(
                            new BigDecimal("100.00"), TipoDatoExportable.DECIMAL))
                    .contains(formateadorValores.formatear(
                            new BigDecimal("-10.00"), TipoDatoExportable.DECIMAL));
        }
    }

    @Test
    @Transactional
    @DisplayName("REP-08: la descarga es la foto de hoy, con la misma tabla y los filtros de la pantalla")
    void exportarStockActual() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO Producto (id_producto, nombre, precio, stock_actual, categoria) "
                        + "VALUES (?, ?, ?, ?, ?)",
                401L, "Café", new BigDecimal("10.00"), new BigDecimal("2.000"), "Bebidas");
        jdbcTemplate.update(
                "INSERT INTO Insumo (id_insumo, nombre, stock_actual, unidad_medida, "
                        + "stock_minimo, costo) VALUES (?, ?, ?, ?, ?, ?)",
                401L, "Harina", new BigDecimal("2.000"), "kg", 5, new BigDecimal("1.00"));

        for (String[] formato : List.of(
                new String[]{"xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"},
                new String[]{"csv", "text/csv;charset=UTF-8"},
                new String[]{"pdf", "application/pdf"})) {
            String extension = formato[0];
            MvcResult resultado = mockMvc.perform(get("/api/reportes/exportar")
                            .param("reporte", "stock-actual")
                            .param("formato", extension)
                            .param("solo_bajo_minimo", "false"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, formato[1]))
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"stock-actual_2026-11-26_2026-11-26."
                                    + extension + "\""))
                    .andReturn();

            String texto = textoExportado(extension, resultado.getResponse().getContentAsByteArray());
            assertThat(texto).describedAs("contenido " + extension)
                    .contains("Reporte de stock actual y bajo mínimo")
                    .contains("Desde: 2026-11-26")
                    .contains("Hasta: 2026-11-26")
                    .contains("Foto al 26/11/2026")
                    .contains("Tipo: Todos")
                    .contains("Categoría: Todas")
                    .contains("Solo bajo mínimo: No")
                    .contains("Emitido: 26/11/2026 22:45")
                    .contains("Artículo")
                    .contains("Stock actual")
                    // El PDF parte las celdas angostas: se verifican fragmentos estables.
                    .contains("Café")
                    .contains("Hari")
                    .contains(formateadorValores.formatear(
                            new BigDecimal("3.000"), TipoDatoExportable.DECIMAL))
                    .contains("Totales");
        }
    }

    @Test
    @Transactional
    @DisplayName("REP-09: la descarga usa la misma tabla y los filtros de la pantalla")
    void exportarMovimientosStock() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO Usuario (id_usuario, nombre, email, password, rol, activo) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                501L, "Ana Stock", "anaStock@taller.local", "hash", "ADMINISTRADOR", true);
        jdbcTemplate.update(
                "INSERT INTO Producto (id_producto, nombre, precio, stock_actual, categoria) "
                        + "VALUES (?, ?, ?, ?, ?)",
                501L, "Café", new BigDecimal("10.00"), new BigDecimal("4.000"), "Bebidas");
        jdbcTemplate.update(
                "INSERT INTO Insumo (id_insumo, nombre, stock_actual, unidad_medida, "
                        + "stock_minimo, costo) VALUES (?, ?, ?, ?, ?, ?)",
                501L, "Harina", new BigDecimal("3.000"), "kg", 1, new BigDecimal("1.00"));
        jdbcTemplate.update(
                "INSERT INTO Proveedor (id_proveedor, nombre, CUIT, telefono, email, tipo_producto) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                501L, "Proveedor A", "20-501-1", "555", "prov501@taller.local", "Insumos");
        jdbcTemplate.update(
                "INSERT INTO Proveedor_Insumo (id_proveedor_insumo, id_proveedor, id_insumo) "
                        + "VALUES (?, ?, ?)",
                501L, 501L, 501L);
        jdbcTemplate.update(
                "INSERT INTO Movimiento_Stock (id_movimiento_stock, id_producto, id_insumo, "
                        + "tipo, cantidad, motivo, id_usuario, fecha_hora, id_venta) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                501L, 501L, null, "ENTRADA", new BigDecimal("4.000"), "COMPRA", 501L,
                java.sql.Timestamp.valueOf("2026-10-01 09:00:00"), null);
        jdbcTemplate.update(
                "INSERT INTO Movimiento_Stock (id_movimiento_stock, id_producto, id_insumo, "
                        + "tipo, cantidad, motivo, id_usuario, fecha_hora, id_venta) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                502L, null, 501L, "ENTRADA", new BigDecimal("3.000"), "COMPRA", 501L,
                java.sql.Timestamp.valueOf("2026-10-02 09:00:00"), null);
        jdbcTemplate.update(
                "INSERT INTO Movimiento_Stock (id_movimiento_stock, id_producto, id_insumo, "
                        + "tipo, cantidad, motivo, id_usuario, fecha_hora, id_venta) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                503L, null, 501L, "SALIDA", new BigDecimal("1.000"), "MERMA", 501L,
                java.sql.Timestamp.valueOf("2026-10-03 17:00:00"), null);

        for (String[] formato : List.of(
                new String[]{"xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"},
                new String[]{"csv", "text/csv;charset=UTF-8"},
                new String[]{"pdf", "application/pdf"})) {
            String extension = formato[0];
            MvcResult resultado = mockMvc.perform(get("/api/reportes/exportar")
                            .param("reporte", "movimientos-stock")
                            .param("formato", extension)
                            .param("desde", "2026-10-01")
                            .param("hasta", "2026-10-31")
                            .param("tipo_articulo", "INSUMO")
                            .param("id_proveedor", "501"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, formato[1]))
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"movimientos-stock_2026-10-01_2026-10-31."
                                    + extension + "\""))
                    .andReturn();

            String texto = textoExportado(extension, resultado.getResponse().getContentAsByteArray());
            assertThat(texto).describedAs("contenido " + extension)
                    .contains("Reporte de entradas, salidas y descartes de stock")
                    .contains("Desde: 2026-10-01")
                    .contains("Hasta: 2026-10-31")
                    .contains("Tipo de artículo: INSUMO")
                    .contains("Proveedor: #501")
                    .contains("Emitido: 26/11/2026 22:45")
                    .contains("Artículo")
                    .contains("Unidad")
                    .contains("Entradas")
                    .contains("Salidas por venta")
                    .contains("Descartes")
                    .contains("Ajustes")
                    .contains("Otras salidas")
                    .contains("Harina")
                    .contains(formateadorValores.formatear(
                            new BigDecimal("3.000"), TipoDatoExportable.DECIMAL))
                    .contains(formateadorValores.formatear(
                            new BigDecimal("1.000"), TipoDatoExportable.DECIMAL))
                    .contains("VEN-10")
                    .contains("Totales")
                    .doesNotContain("Café");
        }
    }

    @Test
    @Transactional
    @DisplayName("REP-10: la descarga usa la tabla por mes y tipo y el nombre lleva períodos")
    void exportarCostosPorTipo() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO Usuario (id_usuario, nombre, email, password, rol, activo) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                901L, "Ana Costos", "anaCostosExport@taller.local", "hash", "ADMINISTRADOR", true);
        jdbcTemplate.update(
                "INSERT INTO tipo_costo (id_tipo_costo, nombre, nombre_normalizado, activo, "
                        + "reservado) VALUES (?, ?, ?, ?, ?)",
                901L, "Alquiler", "alquiler", true, false);
        jdbcTemplate.update(
                "INSERT INTO tipo_costo (id_tipo_costo, nombre, nombre_normalizado, activo, "
                        + "reservado) VALUES (?, ?, ?, ?, ?)",
                902L, "Luz", "luz", true, false);
        jdbcTemplate.update(
                "INSERT INTO Costo (id_costo, id_tipo_costo, descripcion, monto, fecha_pago, "
                        + "periodo_mes, periodo_anio, id_usuario, anulado) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                901L, 901L, "Alquiler octubre", new BigDecimal("120.00"),
                Date.valueOf(LocalDate.of(2026, 10, 15)), 10, 2026, 901L, false);
        jdbcTemplate.update(
                "INSERT INTO Costo (id_costo, id_tipo_costo, descripcion, monto, fecha_pago, "
                        + "periodo_mes, periodo_anio, id_usuario, anulado) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                902L, 902L, "Luz noviembre", new BigDecimal("30.00"),
                Date.valueOf(LocalDate.of(2026, 11, 20)), 11, 2026, 901L, false);

        for (String[] formato : List.of(
                new String[]{"xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"},
                new String[]{"csv", "text/csv;charset=UTF-8"},
                new String[]{"pdf", "application/pdf"})) {
            String extension = formato[0];
            MvcResult resultado = mockMvc.perform(get("/api/reportes/exportar")
                            .param("reporte", "costos-por-tipo")
                            .param("formato", extension)
                            .param("desde", "2026-10")
                            .param("hasta", "2026-11"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, formato[1]))
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"costos-por-tipo_2026-10_2026-11."
                                    + extension + "\""))
                    .andReturn();

            String texto = textoExportado(extension, resultado.getResponse().getContentAsByteArray());
            assertThat(texto).describedAs("contenido " + extension)
                    .contains("Reporte de costos por tipo y su evolución")
                    .contains("Períodos: 2026-10 a 2026-11")
                    .contains("Tipo de costo: Todos")
                    .contains("Costos anulados: excluidos")
                    .contains("Alqu")
                    .contains("Insu")
                    .contains("Luz")
                    .contains("oct 2026")
                    .contains("nov 2026")
                    .contains(formateadorValores.formatear(
                            new BigDecimal("120.00"), TipoDatoExportable.DECIMAL))
                    .contains(formateadorValores.formatear(
                            new BigDecimal("30.00"), TipoDatoExportable.DECIMAL))
                    .contains("CST-06")
                    .contains("Totales");
        }
    }

    /** Convierte cada archivo en texto para poder compararlo con el DTO de la pantalla. */
    private String textoExportado(String formato, byte[] contenido) throws Exception {
        return switch (formato) {
            case "csv" -> new String(contenido, StandardCharsets.UTF_8);
            case "xlsx" -> textoExcel(contenido);
            case "pdf" -> textoPdf(contenido);
            default -> throw new IllegalArgumentException("Formato no soportado: " + formato);
        };
    }

    private String textoExcel(byte[] contenido) throws Exception {
        StringBuilder texto = new StringBuilder();
        try (Workbook libro = WorkbookFactory.create(new ByteArrayInputStream(contenido))) {
            Sheet hoja = libro.getSheetAt(0);
            for (Row fila : hoja) {
                for (Cell celda : fila) {
                    if (celda.getCellType() == CellType.NUMERIC) {
                        texto.append(formateadorValores.formatear(
                                BigDecimal.valueOf(celda.getNumericCellValue()),
                                TipoDatoExportable.DECIMAL));
                    } else if (celda.getCellType() == CellType.STRING) {
                        texto.append(celda.getStringCellValue());
                    }
                    texto.append(' ');
                }
            }
        }
        return texto.toString();
    }

    private String textoPdf(byte[] contenido) throws Exception {
        PdfReader lector = new PdfReader(contenido);
        try {
            StringBuilder texto = new StringBuilder();
            for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
                texto.append(new PdfTextExtractor(lector).getTextFromPage(pagina));
            }
            return texto.toString();
        } finally {
            lector.close();
        }
    }
}

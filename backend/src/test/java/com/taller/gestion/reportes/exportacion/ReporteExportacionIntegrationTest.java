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

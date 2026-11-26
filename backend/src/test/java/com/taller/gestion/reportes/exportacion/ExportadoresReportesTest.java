package com.taller.gestion.reportes.exportacion;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Prueba los tres generadores con el mismo reporte, sin base de datos. */
class ExportadoresReportesTest {

    private static final String EMITIDO = "Emitido: 26/11/2026 22:45";

    private final EncabezadoExportacion encabezado = new EncabezadoExportacion(
            new FormateadorFechaEmision(Clock.fixed(
                    Instant.parse("2026-11-26T22:45:00Z"), ZoneOffset.UTC)));
    private final FormateadorValoresExportacion formateador = new FormateadorValoresExportacion();

    private ReporteExportable reporte() {
        return new ReporteExportable() {
            @Override
            public String id() {
                return "ventas-por-periodo";
            }

            @Override
            public String titulo() {
                return "Ventas por período";
            }

            @Override
            public String descripcionFiltros() {
                return "Agrupado por mes";
            }

            @Override
            public LocalDate desde() {
                return LocalDate.of(2026, 10, 1);
            }

            @Override
            public LocalDate hasta() {
                return LocalDate.of(2026, 10, 31);
            }

            @Override
            public List<ColumnaExportable> columnas() {
                return List.of(
                        new ColumnaExportable("Período", TipoDatoExportable.TEXTO),
                        new ColumnaExportable("Total vendido", TipoDatoExportable.DECIMAL),
                        new ColumnaExportable("Cantidad de ventas", TipoDatoExportable.ENTERO),
                        new ColumnaExportable("Ticket promedio", TipoDatoExportable.DECIMAL));
            }

            @Override
            public List<List<Object>> filasExportables() {
                return List.of(List.<Object>of("oct 2026", new BigDecimal("1500.50"), 3L,
                        new BigDecimal("500.17")));
            }

            @Override
            public List<Object> totalesExportables() {
                return List.of("Totales", new BigDecimal("1500.50"), 3L,
                        new BigDecimal("500.17"));
            }
        };
    }

    @Test
    @DisplayName("CSV: BOM UTF-8, separador ';', coma decimal y fecha de emisión sin zona")
    void csv() {
        ArchivoExportadoDTO archivo =
                new ExportadorCsv(encabezado, formateador).exportar(reporte());
        String texto = new String(archivo.contenido(), StandardCharsets.UTF_8);

        assertThat(archivo.contenido()).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
        assertThat(archivo.contentType()).isEqualTo("text/csv;charset=UTF-8");
        assertThat(texto).contains("Ventas por período")
                .contains("Desde: 2026-10-01")
                .contains("Hasta: 2026-10-31")
                .contains("Agrupado por mes")
                .contains(EMITIDO)
                .doesNotContain("ART");
        assertThat(texto).contains(
                "Período;Total vendido;Cantidad de ventas;Ticket promedio");
        assertThat(texto).contains("oct 2026;1.500,50;3;500,17");
        assertThat(texto).contains("Totales;1.500,50;3;500,17");
    }

    @Test
    @DisplayName("Excel: encabezados, montos como números y fila de totales")
    void excel() throws Exception {
        ArchivoExportadoDTO archivo = new ExportadorExcel(encabezado).exportar(reporte());

        assertThat(archivo.contentType()).isEqualTo(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        try (Workbook libro = WorkbookFactory.create(new ByteArrayInputStream(archivo.contenido()))) {
            Sheet hoja = libro.getSheetAt(0);
            assertThat(hoja.getRow(0).getCell(0).getStringCellValue())
                    .isEqualTo("Ventas por período");
            assertThat(hoja.getRow(4).getCell(0).getStringCellValue()).isEqualTo(EMITIDO);

            Row cabeceras = hoja.getRow(6);
            assertThat(cabeceras.getCell(0).getStringCellValue()).isEqualTo("Período");
            assertThat(cabeceras.getCell(1).getStringCellValue()).isEqualTo("Total vendido");

            Row fila = hoja.getRow(7);
            assertThat(fila.getCell(0).getStringCellValue()).isEqualTo("oct 2026");
            assertThat(fila.getCell(1).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(fila.getCell(1).getNumericCellValue()).isEqualTo(1500.50);
            assertThat(fila.getCell(2).getNumericCellValue()).isEqualTo(3.0);

            Row totales = hoja.getRow(8);
            assertThat(totales.getCell(0).getStringCellValue()).isEqualTo("Totales");
            assertThat(totales.getCell(1).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(totales.getCell(1).getNumericCellValue()).isEqualTo(1500.50);
            assertThat(totales.getCell(2).getNumericCellValue()).isEqualTo(3.0);
        }
    }

    @Test
    @DisplayName("PDF: título, rango, emisión y tabla con los mismos datos")
    void pdf() throws Exception {
        ArchivoExportadoDTO archivo =
                new ExportadorPdf(encabezado, formateador).exportar(reporte());

        assertThat(archivo.contentType()).isEqualTo("application/pdf");
        PdfReader lector = new PdfReader(archivo.contenido());
        try {
            String texto = new PdfTextExtractor(lector).getTextFromPage(1);
            assertThat(texto).contains("Ventas por período")
                    .contains("Desde: 2026-10-01")
                    .contains("Hasta: 2026-10-31")
                    .contains("Agrupado por mes")
                    .contains(EMITIDO)
                    .doesNotContain("ART");
            assertThat(texto).contains("Período")
                    .contains("Total vendido")
                    .contains("oct 2026")
                    .contains("1.500,50")
                    .contains("Totales");
        } finally {
            lector.close();
        }
    }

    @Test
    @DisplayName("El contenido del archivo no se puede modificar desde fuera del DTO")
    void contenidoInmutable() {
        ArchivoExportadoDTO archivo =
                new ExportadorCsv(encabezado, formateador).exportar(reporte());
        byte[] copia = archivo.contenido();
        copia[0] = 0;

        assertThat(archivo.contenido()[0]).isEqualTo((byte) 0xEF);
    }
}

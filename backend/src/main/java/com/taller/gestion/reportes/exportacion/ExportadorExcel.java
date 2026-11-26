package com.taller.gestion.reportes.exportacion;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/** Excel con encabezados, montos como números y una fila de totales. */
@Component
@RequiredArgsConstructor
public class ExportadorExcel implements ExportadorFormato {

    private final EncabezadoExportacion encabezado;

    @Override
    public FormatoExportacion formato() {
        return FormatoExportacion.XLSX;
    }

    @Override
    public ArchivoExportadoDTO exportar(ReporteExportable reporte) {
        XSSFWorkbook libro = new XSSFWorkbook();
        try {
            Estilos estilos = new Estilos(libro);
            Sheet hoja = libro.createSheet("Reporte");

            int filaActual = 0;
            for (String linea : encabezado.lineas(reporte)) {
                Row fila = hoja.createRow(filaActual++);
                Cell celda = fila.createCell(0);
                celda.setCellValue(linea);
                celda.setCellStyle(filaActual == 1 ? estilos.titulo : estilos.texto);
            }
            filaActual++; // fila en blanco entre el encabezado y la tabla

            Row cabeceras = hoja.createRow(filaActual++);
            for (int i = 0; i < reporte.columnas().size(); i++) {
                Cell celda = cabeceras.createCell(i);
                celda.setCellValue(reporte.columnas().get(i).titulo());
                celda.setCellStyle(estilos.cabecera);
            }

            for (List<Object> valores : reporte.filasExportables()) {
                Row fila = hoja.createRow(filaActual++);
                escribir(fila, valores, reporte.columnas(),
                        estilos.texto, estilos.decimal, estilos.entero);
            }

            Row totales = hoja.createRow(filaActual);
            escribir(totales, reporte.totalesExportables(), reporte.columnas(),
                    estilos.totalTexto, estilos.totalDecimal, estilos.totalEntero);

            for (int i = 0; i < reporte.columnas().size(); i++) {
                hoja.autoSizeColumn(i);
            }

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            libro.write(salida);
            return new ArchivoExportadoDTO(salida.toByteArray(),
                    FormatoExportacion.XLSX.contentType(), "reporte.xlsx");
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo generar el Excel del reporte", ex);
        } finally {
            try {
                libro.close();
            } catch (IOException ex) {
                // El archivo ya se escribió; no queremos que un error de cierre tape el resultado.
            }
            // POI y XmlBeans guardan referencias en ThreadLocal; en un pool de hilos
            // del servlet hay que limpiarlas después de cada archivo.
            org.apache.poi.util.ThreadLocalUtil.clearAllThreadLocals();
            org.apache.xmlbeans.ThreadLocalUtil.clearAllThreadLocals();
        }
    }

    private void escribir(Row fila, List<Object> valores, List<ColumnaExportable> columnas,
                          CellStyle texto, CellStyle decimal, CellStyle entero) {
        for (int i = 0; i < valores.size(); i++) {
            TipoDatoExportable tipo = i < columnas.size()
                    ? columnas.get(i).tipo()
                    : TipoDatoExportable.TEXTO;
            Cell celda = fila.createCell(i);
            Object valor = valores.get(i);
            if (valor == null) {
                celda.setBlank();
                continue;
            }
            switch (tipo) {
                case DECIMAL -> {
                    celda.setCellValue(new BigDecimal(valor.toString()).doubleValue());
                    celda.setCellStyle(decimal);
                }
                case ENTERO -> {
                    celda.setCellValue(((Number) valor).longValue());
                    celda.setCellStyle(entero);
                }
                case TEXTO -> {
                    celda.setCellValue(valor.toString());
                    celda.setCellStyle(texto);
                }
            }
        }
    }

    private static final class Estilos {
        private final CellStyle titulo;
        private final CellStyle texto;
        private final CellStyle cabecera;
        private final CellStyle decimal;
        private final CellStyle entero;
        private final CellStyle totalTexto;
        private final CellStyle totalDecimal;
        private final CellStyle totalEntero;

        private Estilos(XSSFWorkbook libro) {
            Font normal = libro.createFont();
            normal.setFontHeightInPoints((short) 10);

            Font negrita = libro.createFont();
            negrita.setBold(true);
            negrita.setFontHeightInPoints((short) 10);

            Font tituloFuente = libro.createFont();
            tituloFuente.setBold(true);
            tituloFuente.setFontHeightInPoints((short) 14);

            titulo = libro.createCellStyle();
            titulo.setFont(tituloFuente);

            texto = libro.createCellStyle();
            texto.setFont(normal);

            cabecera = libro.createCellStyle();
            cabecera.setFont(negrita);
            cabecera.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            cabecera.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            decimal = libro.createCellStyle();
            decimal.setFont(normal);
            decimal.setDataFormat(libro.createDataFormat().getFormat("#,##0.00"));

            entero = libro.createCellStyle();
            entero.setFont(normal);
            entero.setDataFormat(libro.createDataFormat().getFormat("#,##0"));

            totalTexto = libro.createCellStyle();
            totalTexto.setFont(negrita);
            totalTexto.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            totalTexto.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            totalDecimal = libro.createCellStyle();
            totalDecimal.cloneStyleFrom(totalTexto);
            totalDecimal.setDataFormat(libro.createDataFormat().getFormat("#,##0.00"));

            totalEntero = libro.createCellStyle();
            totalEntero.cloneStyleFrom(totalTexto);
            totalEntero.setDataFormat(libro.createDataFormat().getFormat("#,##0"));
        }
    }
}

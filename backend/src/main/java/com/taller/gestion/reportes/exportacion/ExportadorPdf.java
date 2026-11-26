package com.taller.gestion.reportes.exportacion;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

/** PDF con título, rango, fecha de emisión y la tabla del reporte. */
@Component
@RequiredArgsConstructor
public class ExportadorPdf implements ExportadorFormato {

    private final EncabezadoExportacion encabezado;
    private final FormateadorValoresExportacion formateadorValores;

    @Override
    public FormatoExportacion formato() {
        return FormatoExportacion.PDF;
    }

    @Override
    public ArchivoExportadoDTO exportar(ReporteExportable reporte) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4.rotate());
        try {
            documento.setMargins(36, 36, 36, 36);
            PdfWriter.getInstance(documento, salida);
            documento.open();

            Font fuenteTitulo = FontFactory.getFont(FontFactory.HELVETICA, 16, Font.BOLD);
            Font fuenteTexto = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL);
            Font fuenteCabecera = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.BOLD);

            List<String> lineas = encabezado.lineas(reporte);
            documento.add(new Paragraph(lineas.get(0), fuenteTitulo));
            for (int i = 1; i < lineas.size(); i++) {
                documento.add(new Paragraph(lineas.get(i), fuenteTexto));
            }

            List<ColumnaExportable> columnas = reporte.columnas();
            PdfPTable tabla = new PdfPTable(columnas.size());
            tabla.setWidthPercentage(100f);
            tabla.setHeaderRows(1);
            float[] anchos = new float[columnas.size()];
            Arrays.fill(anchos, 1f);
            tabla.setWidths(anchos);

            for (ColumnaExportable columna : columnas) {
                tabla.addCell(celda(columna.titulo(), fuenteCabecera, columna.tipo()));
            }
            for (List<Object> valores : reporte.filasExportables()) {
                agregarFila(tabla, valores, columnas, fuenteTexto);
            }
            agregarFila(tabla, reporte.totalesExportables(), columnas, fuenteCabecera);

            documento.add(tabla);
        } catch (DocumentException ex) {
            throw new IllegalStateException("No se pudo generar el PDF del reporte", ex);
        } finally {
            if (documento.isOpen()) {
                documento.close();
            }
        }
        return new ArchivoExportadoDTO(salida.toByteArray(),
                FormatoExportacion.PDF.contentType(), "reporte.pdf");
    }

    private void agregarFila(PdfPTable tabla, List<Object> valores,
                             List<ColumnaExportable> columnas, Font fuente) {
        for (int i = 0; i < valores.size(); i++) {
            TipoDatoExportable tipo = i < columnas.size()
                    ? columnas.get(i).tipo()
                    : TipoDatoExportable.TEXTO;
            tabla.addCell(celda(
                    formateadorValores.formatear(valores.get(i), tipo), fuente, tipo));
        }
    }

    private PdfPCell celda(String texto, Font fuente, TipoDatoExportable tipo) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, fuente));
        celda.setHorizontalAlignment(tipo == TipoDatoExportable.TEXTO
                ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
        celda.setPadding(4f);
        return celda;
    }
}

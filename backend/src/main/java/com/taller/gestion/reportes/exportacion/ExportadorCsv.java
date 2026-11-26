package com.taller.gestion.reportes.exportacion;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** CSV para Excel en español: UTF-8 con BOM, separador ';' y coma decimal. */
@Component
@RequiredArgsConstructor
public class ExportadorCsv implements ExportadorFormato {

    private static final byte[] BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final String SALTO = "\r\n";

    private final EncabezadoExportacion encabezado;
    private final FormateadorValoresExportacion formateadorValores;

    @Override
    public FormatoExportacion formato() {
        return FormatoExportacion.CSV;
    }

    @Override
    public ArchivoExportadoDTO exportar(ReporteExportable reporte) {
        StringBuilder texto = new StringBuilder();
        for (String linea : encabezado.lineas(reporte)) {
            agregarFila(texto, List.of(linea));
        }
        agregarFila(texto, List.of());

        List<String> titulos = reporte.columnas().stream()
                .map(ColumnaExportable::titulo)
                .toList();
        agregarFila(texto, titulos);

        for (List<Object> fila : reporte.filasExportables()) {
            agregarFila(texto, celdas(fila, reporte.columnas()));
        }
        agregarFila(texto, celdas(reporte.totalesExportables(), reporte.columnas()));

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        salida.write(BOM, 0, BOM.length);
        byte[] contenido = texto.toString().getBytes(StandardCharsets.UTF_8);
        salida.write(contenido, 0, contenido.length);
        return new ArchivoExportadoDTO(
                salida.toByteArray(), FormatoExportacion.CSV.contentType(), "reporte.csv");
    }

    private List<String> celdas(List<Object> valores, List<ColumnaExportable> columnas) {
        List<String> resultado = new ArrayList<>(valores.size());
        for (int i = 0; i < valores.size(); i++) {
            TipoDatoExportable tipo = i < columnas.size()
                    ? columnas.get(i).tipo()
                    : TipoDatoExportable.TEXTO;
            resultado.add(formateadorValores.formatear(valores.get(i), tipo));
        }
        return resultado;
    }

    private void agregarFila(StringBuilder texto, List<String> celdas) {
        for (int i = 0; i < celdas.size(); i++) {
            if (i > 0) {
                texto.append(';');
            }
            texto.append(escapar(celdas.get(i)));
        }
        texto.append(SALTO);
    }

    private String escapar(String valor) {
        boolean entreComillas = valor.contains(";") || valor.contains("\"")
                || valor.contains("\n") || valor.contains("\r");
        String escapado = valor.replace("\"", "\"\"");
        return entreComillas ? "\"" + escapado + "\"" : escapado;
    }
}

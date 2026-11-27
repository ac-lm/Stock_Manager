package com.taller.gestion.reportes.dto;

import com.taller.gestion.reportes.exportacion.ColumnaExportable;
import com.taller.gestion.reportes.exportacion.ReporteExportable;
import com.taller.gestion.reportes.exportacion.TipoDatoExportable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Schema(description = "Foto de hoy del stock de productos e insumos, con los que están bajo mínimo")
public record ReporteStockActualDTO(
        @Schema(description = "Día de la foto, en la zona del taller; no es un rango")
        LocalDate desde,

        @Schema(description = "Igual que desde: el reporte no lleva rango de fechas")
        LocalDate hasta,

        @Schema(description = "TODOS, PRODUCTO o INSUMO")
        String tipo,

        @Schema(description = "Categoría aplicada; null si se muestran todas", nullable = true)
        String categoria,

        @Schema(description = "True si la tabla solo incluye artículos bajo mínimo")
        boolean soloBajoMinimo,

        @Schema(description = "Filas de la tabla")
        List<StockActualFilaDTO> filas,

        @Schema(description = "Bajo mínimo ordenados por mayor falta; alimenta el gráfico")
        List<StockActualFilaDTO> bajoMinimo,

        @Schema(description = "Cantidades del reporte, sin sumar stocks")
        TotalesStockActualDTO totales
) implements ReporteExportable {

    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("es-AR"));
    private static final String NO_APLICA = "—";

    @Override
    public String id() {
        return "stock-actual";
    }

    @Override
    public String titulo() {
        return "Reporte de stock actual y bajo mínimo";
    }

    @Override
    public String descripcionFiltros() {
        List<String> partes = new ArrayList<>();
        partes.add("Foto al " + FECHA.format(desde));
        partes.add("Tipo: " + etiquetaTipo(tipo));
        partes.add("Categoría: " + (categoria == null ? "Todas" : categoria));
        partes.add("Solo bajo mínimo: " + (soloBajoMinimo ? "Sí" : "No"));
        partes.add("Artículos: " + totales.articulos());
        partes.add("Bajo mínimo: " + totales.bajoMinimo());
        return String.join("; ", partes);
    }

    @Override
    public List<ColumnaExportable> columnas() {
        return List.of(
                new ColumnaExportable("Tipo", TipoDatoExportable.TEXTO),
                new ColumnaExportable("Artículo", TipoDatoExportable.TEXTO),
                new ColumnaExportable("Unidad", TipoDatoExportable.TEXTO),
                new ColumnaExportable("Categoría", TipoDatoExportable.TEXTO),
                new ColumnaExportable("Stock actual", TipoDatoExportable.DECIMAL),
                new ColumnaExportable("Stock mínimo", TipoDatoExportable.DECIMAL),
                new ColumnaExportable("Falta para el mínimo", TipoDatoExportable.DECIMAL));
    }

    @Override
    public List<List<Object>> filasExportables() {
        return filas.stream()
                .map(fila -> Arrays.<Object>asList(
                        fila.tipo().name(),
                        fila.articulo(),
                        fila.unidad() == null ? NO_APLICA : fila.unidad(),
                        fila.categoria() == null ? NO_APLICA : fila.categoria(),
                        fila.stockActual(),
                        fila.stockMinimo() == null ? null : BigDecimal.valueOf(fila.stockMinimo()),
                        fila.falta()))
                .toList();
    }

    @Override
    public List<Object> totalesExportables() {
        // No se suman stocks de unidades distintas: las celdas numéricas quedan vacías.
        return Arrays.asList(
                "Totales",
                totales.articulos() + " artículos",
                "", "",
                null, null, null);
    }

    private String etiquetaTipo(String valor) {
        if (valor == null || valor.isBlank() || "TODOS".equalsIgnoreCase(valor)) {
            return "Todos";
        }
        return valor.substring(0, 1).toUpperCase(Locale.ROOT)
                + valor.substring(1).toLowerCase(Locale.ROOT);
    }
}

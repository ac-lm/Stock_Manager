package com.taller.gestion.reportes.dto;

import com.taller.gestion.reportes.exportacion.ColumnaExportable;
import com.taller.gestion.reportes.exportacion.ReporteExportable;
import com.taller.gestion.reportes.exportacion.TipoDatoExportable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Schema(description = "Evolución mensual de las ventas y comparación entre años "
        + "con una línea por año")
public record ReporteComparacionEstacionalDTO(
        @Schema(description = "Años comparados, ordenados de menor a mayor", example = "[2024, 2025, 2026]")
        List<Integer> anios,

        @Schema(description = "Producto filtrado, o null si se muestran todos", example = "7")
        Long id_producto,

        @Schema(description = "Categoría filtrada, o null si se muestran todas", example = "Bebidas")
        String categoria,

        @Schema(description = "Primer día del rango, incluido", example = "2024-01-01")
        LocalDate desde,

        @Schema(description = "Último día del rango, incluido. En el año en curso es el último mes pasado",
                example = "2026-11-30")
        LocalDate hasta,

        @Schema(description = "Una fila por mes mostrado, con enero a diciembre del primer año completo")
        List<ComparacionEstacionalFilaDTO> filas,

        @Schema(description = "Totales por año y su variación")
        TotalesComparacionEstacionalDTO totales
) implements ReporteExportable {

    private static final Locale LOCALE = Locale.forLanguageTag("es-AR");
    private static final String SIN_VARIACION = "—";

    @Override
    public String id() {
        return "evolucion-ventas";
    }

    @Override
    public String titulo() {
        return "Evolución de ventas y comparación estacional";
    }

    @Override
    public String descripcionFiltros() {
        List<String> partes = new ArrayList<>();
        partes.add("Años: " + anios.stream().map(String::valueOf).collect(Collectors.joining(", ")));
        if (id_producto != null) {
            partes.add("Producto: " + id_producto);
        }
        if (categoria != null && !categoria.isBlank()) {
            partes.add("Categoría: " + categoria);
        }
        return String.join("; ", partes);
    }

    @Override
    public List<ColumnaExportable> columnas() {
        List<ColumnaExportable> columnas = new ArrayList<>();
        columnas.add(new ColumnaExportable("Mes", TipoDatoExportable.TEXTO));
        for (int anio : anios) {
            columnas.add(new ColumnaExportable(String.valueOf(anio), TipoDatoExportable.DECIMAL));
        }
        for (int i = 1; i < anios.size(); i++) {
            columnas.add(new ColumnaExportable(
                    "Var. " + anios.get(i) + " / " + anios.get(i - 1), TipoDatoExportable.TEXTO));
        }
        return List.copyOf(columnas);
    }

    @Override
    public List<List<Object>> filasExportables() {
        return filas.stream()
                .map(fila -> valoresFila(fila.etiqueta(), fila.totalesPorAnio(),
                        fila.variacionesPorAnio()))
                .toList();
    }

    @Override
    public List<Object> totalesExportables() {
        return valoresFila("Totales", totales.totalesPorAnio(), totales.variacionesPorAnio());
    }

    private List<Object> valoresFila(String etiqueta, Map<String, BigDecimal> totales,
                                     Map<String, BigDecimal> variaciones) {
        List<Object> valores = new ArrayList<>();
        valores.add(etiqueta);
        for (int anio : anios) {
            valores.add(totales.getOrDefault(String.valueOf(anio), BigDecimal.ZERO.setScale(2)));
        }
        for (int i = 1; i < anios.size(); i++) {
            valores.add(textoVariacion(variaciones.get(String.valueOf(anios.get(i)))));
        }
        return List.copyOf(valores);
    }

    private String textoVariacion(BigDecimal valor) {
        if (valor == null) {
            return SIN_VARIACION;
        }
        return String.format(LOCALE, "%.2f %%", valor);
    }
}

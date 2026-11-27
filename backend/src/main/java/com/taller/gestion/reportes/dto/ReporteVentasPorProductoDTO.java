package com.taller.gestion.reportes.dto;

import com.taller.gestion.reportes.exportacion.ColumnaExportable;
import com.taller.gestion.reportes.exportacion.ReporteExportable;
import com.taller.gestion.reportes.exportacion.TipoDatoExportable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "Ventas por producto y ranking de los más vendidos del rango")
public record ReporteVentasPorProductoDTO(
        @Schema(description = "Primer día del rango, incluido", example = "2026-10-01")
        LocalDate desde,

        @Schema(description = "Último día del rango, incluido", example = "2026-10-31")
        LocalDate hasta,

        @Schema(description = "Producto filtrado, o null si se muestran todos", example = "7")
        Long id_producto,

        @Schema(description = "Categoría filtrada, o null si se muestran todas", example = "Bebidas")
        String categoria,

        @Schema(description = "Cantidad de productos del ranking", example = "10")
        int top,

        @Schema(description = "Una fila por producto con ventas en el rango")
        List<VentasPorProductoFilaDTO> filas,

        @Schema(description = "Los N productos con mayor total vendido")
        List<VentasPorProductoFilaDTO> ranking,

        @Schema(description = "Totales del rango completo")
        TotalesVentasPorProductoDTO totales
) implements ReporteExportable {

    private static final List<ColumnaExportable> COLUMNAS = List.of(
            new ColumnaExportable("Producto", TipoDatoExportable.TEXTO),
            new ColumnaExportable("Categoría", TipoDatoExportable.TEXTO),
            new ColumnaExportable("Cantidad vendida", TipoDatoExportable.DECIMAL),
            new ColumnaExportable("Total vendido", TipoDatoExportable.DECIMAL));

    @Override
    public String id() {
        return "ventas-por-producto";
    }

    @Override
    public String titulo() {
        return "Ventas por producto";
    }

    @Override
    public String descripcionFiltros() {
        List<String> partes = new ArrayList<>();
        if (id_producto != null) {
            partes.add("Producto: " + id_producto);
        }
        if (categoria != null && !categoria.isBlank()) {
            partes.add("Categoría: " + categoria);
        }
        partes.add("Top " + top + " por total vendido");
        return String.join("; ", partes);
    }

    @Override
    public List<ColumnaExportable> columnas() {
        return COLUMNAS;
    }

    @Override
    public List<List<Object>> filasExportables() {
        return filas.stream()
                .map(fila -> List.<Object>of(fila.producto(), fila.categoria(),
                        fila.cantidadVendida(), fila.totalVendido()))
                .toList();
    }

    @Override
    public List<Object> totalesExportables() {
        return List.of("Totales", "", totales.cantidadVendida(), totales.totalVendido());
    }
}

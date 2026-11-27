package com.taller.gestion.reportes.dto;

import com.taller.gestion.reportes.exportacion.ColumnaExportable;
import com.taller.gestion.reportes.exportacion.ReporteExportable;
import com.taller.gestion.reportes.exportacion.TipoDatoExportable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Ventas por vendedor del rango")
public record ReporteVentasPorVendedorDTO(
        @Schema(description = "Primer día del rango, incluido", example = "2026-10-01")
        LocalDate desde,

        @Schema(description = "Último día del rango, incluido", example = "2026-10-31")
        LocalDate hasta,

        @Schema(description = "Vendedor filtrado, o null si se muestran todos", example = "7")
        Long id_vendedor,

        @Schema(description = "Una fila por vendedor con ventas en el rango, ordenada por total")
        List<VentasPorVendedorFilaDTO> filas,

        @Schema(description = "Totales del rango completo")
        TotalesVentasPorVendedorDTO totales
) implements ReporteExportable {

    private static final List<ColumnaExportable> COLUMNAS = List.of(
            new ColumnaExportable("Vendedor", TipoDatoExportable.TEXTO),
            new ColumnaExportable("Cantidad de ventas", TipoDatoExportable.ENTERO),
            new ColumnaExportable("Total vendido", TipoDatoExportable.DECIMAL),
            new ColumnaExportable("Ticket promedio", TipoDatoExportable.DECIMAL));

    @Override
    public String id() {
        return "ventas-por-vendedor";
    }

    @Override
    public String titulo() {
        return "Ventas por vendedor";
    }

    @Override
    public String descripcionFiltros() {
        return id_vendedor == null ? "" : "Vendedor: " + id_vendedor;
    }

    @Override
    public List<ColumnaExportable> columnas() {
        return COLUMNAS;
    }

    @Override
    public List<List<Object>> filasExportables() {
        return filas.stream()
                .map(fila -> List.<Object>of(fila.vendedor(), fila.cantidadVentas(),
                        fila.totalVendido(), fila.ticketPromedio()))
                .toList();
    }

    @Override
    public List<Object> totalesExportables() {
        return List.of("Totales", totales.cantidadVentas(),
                totales.totalVendido(), totales.ticketPromedio());
    }
}

package com.taller.gestion.reportes.dto;

import com.taller.gestion.reportes.exportacion.ColumnaExportable;
import com.taller.gestion.reportes.exportacion.ReporteExportable;
import com.taller.gestion.reportes.exportacion.TipoDatoExportable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Reporte de ventas por período. Los períodos sin ventas se devuelven en cero.")
public record ReporteVentasPorPeriodoDTO(
        @Schema(description = "Primer día del rango, incluido", example = "2026-10-01")
        LocalDate desde,

        @Schema(description = "Último día del rango, incluido", example = "2026-10-31")
        LocalDate hasta,

        @Schema(description = "Agrupación de las filas")
        AgrupacionPeriodo agrupacion,

        @Schema(description = "Filas del reporte, una por período del rango")
        List<VentasPorPeriodoFilaDTO> filas,

        @Schema(description = "Totales del rango completo")
        TotalesVentasPorPeriodoDTO totales
) implements ReporteExportable {

    private static final List<ColumnaExportable> COLUMNAS = List.of(
            new ColumnaExportable("Período", TipoDatoExportable.TEXTO),
            new ColumnaExportable("Total vendido", TipoDatoExportable.DECIMAL),
            new ColumnaExportable("Cantidad de ventas", TipoDatoExportable.ENTERO),
            new ColumnaExportable("Ticket promedio", TipoDatoExportable.DECIMAL));

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
        String agrupamiento = switch (agrupacion) {
            case DIA -> "día";
            case SEMANA -> "semana";
            case MES -> "mes";
        };
        return "Agrupado por " + agrupamiento;
    }

    @Override
    public List<ColumnaExportable> columnas() {
        return COLUMNAS;
    }

    @Override
    public List<List<Object>> filasExportables() {
        return filas.stream()
                .map(fila -> List.<Object>of(fila.etiqueta(), fila.totalVendido(),
                        fila.cantidadVentas(), fila.ticketPromedio()))
                .toList();
    }

    @Override
    public List<Object> totalesExportables() {
        return List.of("Totales", totales.totalVendido(), totales.cantidadVentas(),
                totales.ticketPromedio());
    }
}

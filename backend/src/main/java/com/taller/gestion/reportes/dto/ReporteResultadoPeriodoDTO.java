package com.taller.gestion.reportes.dto;

import com.taller.gestion.reportes.exportacion.ColumnaExportable;
import com.taller.gestion.reportes.exportacion.ReporteExportable;
import com.taller.gestion.reportes.exportacion.TipoDatoExportable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Resultado mensual: ventas confirmadas menos costos del período")
public record ReporteResultadoPeriodoDTO(
        @Schema(description = "Primer día del primer mes del rango")
        LocalDate desde,

        @Schema(description = "Último día del último mes del rango")
        LocalDate hasta,

        @Schema(description = "Primer mes del rango en formato YYYY-MM", example = "2026-10")
        String desdePeriodo,

        @Schema(description = "Último mes del rango en formato YYYY-MM", example = "2026-12")
        String hastaPeriodo,

        @Schema(description = "Una fila por mes del rango, con los vacíos en 0")
        List<ResultadoPeriodoFilaDTO> filas,

        @Schema(description = "Totales generales del rango")
        TotalesResultadoPeriodoDTO totales,

        @Schema(description = "Aclaración de que es un resultado simple, no un balance contable")
        String aclaracion,

        @Schema(description = "Aviso sobre la evolución de insumos mientras CST-06 no esté listo")
        String avisoInsumos
) implements ReporteExportable {

    private static final List<ColumnaExportable> COLUMNAS = List.of(
            new ColumnaExportable("Período", TipoDatoExportable.TEXTO),
            new ColumnaExportable("Total vendido", TipoDatoExportable.DECIMAL),
            new ColumnaExportable("Total costos", TipoDatoExportable.DECIMAL),
            new ColumnaExportable("Resultado", TipoDatoExportable.DECIMAL));

    @Override
    public String id() {
        return "resultado-periodo";
    }

    @Override
    public String titulo() {
        return "Resultado del período (ventas menos costos)";
    }

    @Override
    public String descripcionFiltros() {
        return "Períodos: " + desdePeriodo + " a " + hastaPeriodo
                + "; Ventas: fecha de venta, solo confirmadas"
                + "; Costos: periodo_anio/periodo_mes, anulados excluidos"
                + "; " + aclaracion
                + "; " + avisoInsumos;
    }

    @Override
    public String nombreRango() {
        return desdePeriodo + "_" + hastaPeriodo;
    }

    @Override
    public List<ColumnaExportable> columnas() {
        return COLUMNAS;
    }

    @Override
    public List<List<Object>> filasExportables() {
        return filas.stream()
                .map(fila -> List.<Object>of(fila.etiqueta(), fila.totalVendido(),
                        fila.totalCostos(), fila.resultado()))
                .toList();
    }

    @Override
    public List<Object> totalesExportables() {
        return List.of("Totales", totales.totalVendido(), totales.totalCostos(),
                totales.resultado());
    }
}

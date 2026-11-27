package com.taller.gestion.reportes.dto;

import com.taller.gestion.reportes.exportacion.ColumnaExportable;
import com.taller.gestion.reportes.exportacion.ReporteExportable;
import com.taller.gestion.reportes.exportacion.TipoDatoExportable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "Costos por tipo y mes, con los meses vacíos completados en 0")
public record ReporteCostosPorTipoDTO(
        @Schema(description = "Primer día del primer mes del rango")
        LocalDate desde,

        @Schema(description = "Último día del último mes del rango")
        LocalDate hasta,

        @Schema(description = "Primer mes del rango en formato YYYY-MM", example = "2026-10")
        String desdePeriodo,

        @Schema(description = "Último mes del rango en formato YYYY-MM", example = "2026-12")
        String hastaPeriodo,

        @Schema(description = "Id del tipo filtrado; null si se muestran todos", nullable = true)
        Long idTipoCosto,

        @Schema(description = "Tipos disponibles en el selector: activos, inactivos con costos "
                + "en el rango y el reservado Insumos")
        List<TipoCostoOpcionDTO> tiposDisponibles,

        @Schema(description = "Tipos mostrados en la tabla y en el gráfico, con su total")
        List<TipoCostoReporteDTO> tipos,

        @Schema(description = "Una fila por mes del rango")
        List<CostoPorTipoFilaDTO> filas,

        @Schema(description = "Totales generales del rango")
        TotalesCostosPorTipoDTO totales,

        @Schema(description = "Aviso sobre la evolución de insumos mientras CST-06 no esté listo")
        String avisoInsumos
) implements ReporteExportable {

    @Override
    public String id() {
        return "costos-por-tipo";
    }

    @Override
    public String titulo() {
        return "Reporte de costos por tipo y su evolución";
    }

    @Override
    public String descripcionFiltros() {
        return "Períodos: " + desdePeriodo + " a " + hastaPeriodo
                + "; Tipo de costo: " + (idTipoCosto == null ? "Todos" : "#" + idTipoCosto)
                + "; Costos anulados: excluidos"
                + "; Agrupado por periodo_anio/periodo_mes (fecha_pago no interviene)"
                + "; Costos: " + totales.registros()
                + "; " + avisoInsumos;
    }

    @Override
    public String nombreRango() {
        return desdePeriodo + "_" + hastaPeriodo;
    }

    @Override
    public List<ColumnaExportable> columnas() {
        List<ColumnaExportable> columnas = new ArrayList<>();
        columnas.add(new ColumnaExportable("Período", TipoDatoExportable.TEXTO));
        for (TipoCostoReporteDTO tipo : tipos) {
            columnas.add(new ColumnaExportable(tipo.nombre(), TipoDatoExportable.DECIMAL));
        }
        columnas.add(new ColumnaExportable("Total", TipoDatoExportable.DECIMAL));
        return columnas;
    }

    @Override
    public List<List<Object>> filasExportables() {
        List<List<Object>> exportables = new ArrayList<>();
        for (CostoPorTipoFilaDTO fila : filas) {
            List<Object> valores = new ArrayList<>();
            valores.add(fila.etiqueta());
            for (TipoCostoReporteDTO tipo : tipos) {
                valores.add(fila.valoresPorTipo()
                        .getOrDefault(String.valueOf(tipo.id()), BigDecimal.ZERO));
            }
            valores.add(fila.total());
            exportables.add(valores);
        }
        return exportables;
    }

    @Override
    public List<Object> totalesExportables() {
        List<Object> valores = new ArrayList<>();
        valores.add("Totales");
        for (TipoCostoReporteDTO tipo : tipos) {
            valores.add(tipo.total());
        }
        valores.add(totales.totalGeneral());
        return valores;
    }
}

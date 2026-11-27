package com.taller.gestion.reportes.dto;

import com.taller.gestion.reportes.exportacion.ColumnaExportable;
import com.taller.gestion.reportes.exportacion.ReporteExportable;
import com.taller.gestion.reportes.exportacion.TipoDatoExportable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/** Incidencias del rango contra los descartes MERMA del mismo artículo, sin mezclar unidades. */
@Schema(description = "Incidencias y descartes de stock por tipo, artículo y estado")
public record ReporteIncidenciasDescartesDTO(
        @Schema(description = "Primer día del rango, incluido")
        LocalDate desde,

        @Schema(description = "Último día del rango, incluido")
        LocalDate hasta,

        @Schema(description = "Id del tipo elegido; null si se muestran todos", nullable = true)
        Long idTipoIncidencia,

        @Schema(description = "TODOS, ABIERTA o CERRADA")
        String estado,

        @Schema(description = "TODOS, PRODUCTO o INSUMO")
        String tipoArticulo,

        @Schema(description = "Id del artículo elegido; null si se muestran todos", nullable = true)
        Long idArticulo,

        @Schema(description = "Tipos disponibles en el rango para el selector")
        List<TipoIncidenciaOpcionDTO> tiposDisponibles,

        @Schema(description = "Filas de la tabla, una por tipo, artículo y estado")
        List<IncidenciaDescartesFilaDTO> filas,

        @Schema(description = "Solo conteos; las cantidades no se suman entre artículos")
        TotalesIncidenciasDescartesDTO totales,

        @Schema(description = "Incidencias distintas por tipo, para el gráfico; sin descartes")
        List<ConteoIncidenciaPorTipoDTO> incidenciasPorTipo,

        @Schema(description = "Avisos sobre el cruce agregado y el estado de INC-07")
        List<String> avisos
) implements ReporteExportable {

    private static final String NO_APLICA = "—";

    @Override
    public String id() {
        return "incidencias-descartes";
    }

    @Override
    public String titulo() {
        return "Incidencias y descartes";
    }

    @Override
    public String descripcionFiltros() {
        return "Tipo de incidencia: "
                + (idTipoIncidencia == null ? "Todos" : "#" + idTipoIncidencia)
                + "; Estado: " + estado
                + "; Tipo de artículo: " + tipoArticulo
                + "; Artículo: " + (idArticulo == null ? "Todos" : "#" + idArticulo)
                + "; Incidencias: " + totales.incidencias()
                + "; Abiertas: " + totales.abiertas()
                + "; Cerradas: " + totales.cerradas()
                + "; Movimientos MERMA: " + totales.movimientosMerma()
                + "; " + String.join("; ", avisos);
    }

    @Override
    public List<ColumnaExportable> columnas() {
        return List.of(
                new ColumnaExportable("Tipo de incidencia", TipoDatoExportable.TEXTO),
                new ColumnaExportable("Artículo", TipoDatoExportable.TEXTO),
                new ColumnaExportable("Unidad", TipoDatoExportable.TEXTO),
                new ColumnaExportable("Estado", TipoDatoExportable.TEXTO),
                new ColumnaExportable("Incidencias", TipoDatoExportable.ENTERO),
                new ColumnaExportable("Cantidad afectada", TipoDatoExportable.DECIMAL),
                new ColumnaExportable("Abiertas", TipoDatoExportable.ENTERO),
                new ColumnaExportable("Cerradas", TipoDatoExportable.ENTERO),
                new ColumnaExportable("Movimientos MERMA", TipoDatoExportable.ENTERO),
                new ColumnaExportable("Cantidad descartada", TipoDatoExportable.DECIMAL),
                new ColumnaExportable("Posible traslape", TipoDatoExportable.TEXTO));
    }

    @Override
    public List<List<Object>> filasExportables() {
        return filas.stream()
                .map(fila -> Arrays.<Object>asList(
                        fila.tipoIncidencia(),
                        fila.articulo(),
                        fila.unidad() == null ? NO_APLICA : fila.unidad(),
                        fila.estado(),
                        fila.incidencias(),
                        fila.cantidadAfectada(),
                        fila.abiertas(),
                        fila.cerradas(),
                        fila.movimientosMerma(),
                        fila.cantidadDescartada(),
                        fila.posibleTraslape() ? "Sí" : "No"))
                .toList();
    }

    @Override
    public List<Object> totalesExportables() {
        return Arrays.asList("Totales", NO_APLICA, NO_APLICA, NO_APLICA,
                totales.incidencias(), null, totales.abiertas(), totales.cerradas(),
                totales.movimientosMerma(), null, NO_APLICA);
    }
}

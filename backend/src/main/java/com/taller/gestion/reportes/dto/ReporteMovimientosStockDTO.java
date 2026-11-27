package com.taller.gestion.reportes.dto;

import com.taller.gestion.reportes.exportacion.ColumnaExportable;
import com.taller.gestion.reportes.exportacion.ReporteExportable;
import com.taller.gestion.reportes.exportacion.TipoDatoExportable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Schema(description = "Movimientos de stock del rango, agrupados por artículo")
public record ReporteMovimientosStockDTO(
        @Schema(description = "Primer día del rango, incluido")
        LocalDate desde,

        @Schema(description = "Último día del rango, incluido")
        LocalDate hasta,

        @Schema(description = "TODOS, PRODUCTO o INSUMO")
        String tipoArticulo,

        @Schema(description = "Id del artículo elegido; null si se muestran todos", nullable = true)
        Long idArticulo,

        @Schema(description = "TODOS, ENTRADA o SALIDA")
        String tipoMovimiento,

        @Schema(description = "Id del proveedor elegido; null si se muestran todos", nullable = true)
        Long idProveedor,

        @Schema(description = "Filas de la tabla, una por artículo")
        List<MovimientoStockFilaDTO> filas,

        @Schema(description = "Cantidades del reporte, sin sumar unidades distintas")
        TotalesMovimientosStockDTO totales
) implements ReporteExportable {

    private static final String NO_APLICA = "—";

    @Override
    public String id() {
        return "movimientos-stock";
    }

    @Override
    public String titulo() {
        return "Reporte de entradas, salidas y descartes de stock";
    }

    @Override
    public String descripcionFiltros() {
        return "Tipo de artículo: " + tipoArticulo
                + "; Artículo: " + (idArticulo == null ? "Todos" : "#" + idArticulo)
                + "; Tipo de movimiento: " + tipoMovimiento
                + "; Proveedor: " + (idProveedor == null ? "Todos" : "#" + idProveedor)
                + "; Artículos: " + totales.articulos()
                + "; Movimientos: " + totales.movimientos()
                + "; Las salidas por venta aparecerán cuando se implemente VEN-10";
    }

    @Override
    public List<ColumnaExportable> columnas() {
        return List.of(
                new ColumnaExportable("Artículo", TipoDatoExportable.TEXTO),
                new ColumnaExportable("Unidad", TipoDatoExportable.TEXTO),
                new ColumnaExportable("Entradas", TipoDatoExportable.DECIMAL),
                new ColumnaExportable("Salidas por venta", TipoDatoExportable.DECIMAL),
                new ColumnaExportable("Descartes", TipoDatoExportable.DECIMAL),
                new ColumnaExportable("Ajustes", TipoDatoExportable.DECIMAL),
                new ColumnaExportable("Otras salidas", TipoDatoExportable.DECIMAL));
    }

    @Override
    public List<List<Object>> filasExportables() {
        return filas.stream()
                .map(fila -> Arrays.<Object>asList(
                        fila.articulo(),
                        fila.unidad() == null ? NO_APLICA : fila.unidad(),
                        fila.entradas(), fila.salidasVenta(), fila.descartes(),
                        fila.ajustes(), fila.otrasSalidas()))
                .toList();
    }

    @Override
    public List<Object> totalesExportables() {
        // No se suman cantidades de unidades distintas: solo se informan los conteos.
        // Los conteos van en columnas de texto; las cinco columnas numéricas quedan vacías.
        return Arrays.asList("Totales",
                totales.articulos() + " artículos, " + totales.movimientos() + " movimientos",
                null, null, null, null, null);
    }
}

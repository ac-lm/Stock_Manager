package com.taller.gestion.reportes.dto;

import com.taller.gestion.reportes.exportacion.ColumnaExportable;
import com.taller.gestion.reportes.exportacion.ReporteExportable;
import com.taller.gestion.reportes.exportacion.TipoDatoExportable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "Ventas por cliente y resumen por tipo de cliente del rango")
public record ReporteVentasPorClienteDTO(
        @Schema(description = "Primer día del rango, incluido", example = "2026-10-01")
        LocalDate desde,

        @Schema(description = "Último día del rango, incluido", example = "2026-10-31")
        LocalDate hasta,

        @Schema(description = "Cliente filtrado, o null si se muestran todos", example = "7")
        Long id_cliente,

        @Schema(description = "Tipo filtrado: GRAN_CLIENTE o CONSUMIDOR_FINAL (incluye el genérico), "
                + "o null si se muestran todos", example = "CONSUMIDOR_FINAL")
        String tipo_cliente,

        @Schema(description = "Una fila por cliente con ventas en el rango, ordenada por total")
        List<VentasPorClienteFilaDTO> filas,

        @Schema(description = "Total por tipo de cliente; el genérico se suma a Consumidor final")
        List<TotalesPorTipoClienteDTO> porTipo,

        @Schema(description = "Totales del rango completo")
        TotalesVentasPorClienteDTO totales
) implements ReporteExportable {

    private static final List<ColumnaExportable> COLUMNAS = List.of(
            new ColumnaExportable("Cliente", TipoDatoExportable.TEXTO),
            new ColumnaExportable("Tipo", TipoDatoExportable.TEXTO),
            new ColumnaExportable("Cantidad de compras", TipoDatoExportable.ENTERO),
            new ColumnaExportable("Total vendido", TipoDatoExportable.DECIMAL));

    @Override
    public String id() {
        return "ventas-por-cliente";
    }

    @Override
    public String titulo() {
        return "Ventas por cliente";
    }

    @Override
    public String descripcionFiltros() {
        List<String> partes = new ArrayList<>();
        if (id_cliente != null) {
            partes.add("Cliente: " + id_cliente);
        }
        if (tipo_cliente != null && !tipo_cliente.isBlank()) {
            partes.add("Tipo de cliente: " + ("GRAN_CLIENTE".equals(tipo_cliente)
                    ? "Gran cliente" : "Consumidor final"));
        }
        return String.join("; ", partes);
    }

    @Override
    public List<ColumnaExportable> columnas() {
        return COLUMNAS;
    }

    @Override
    public List<List<Object>> filasExportables() {
        return filas.stream()
                .map(fila -> List.<Object>of(fila.cliente(), fila.tipo(),
                        fila.cantidadCompras(), fila.totalVendido()))
                .toList();
    }

    @Override
    public List<Object> totalesExportables() {
        return List.of("Totales", "", totales.cantidadCompras(), totales.totalVendido());
    }
}

package com.taller.gestion.reportes.dto;

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
) {
}

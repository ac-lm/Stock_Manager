package com.taller.gestion.reportes.controller;

import com.taller.gestion.common.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.Map;

@Tag(name = "Reportes",
        description = "Consultas de solo lectura sobre los datos de otros módulos. "
                + "Requiere una sesión iniciada con rol ADMINISTRADOR.")
public interface ReporteExportacionApi {

    @Operation(summary = "Exportar un reporte",
            description = "Genera el mismo reporte que muestra la pantalla, con los mismos filtros, "
                    + "en xlsx, csv o pdf. Los filtros se pasan como parámetros de consulta; "
                    + "por ejemplo: reporte=ventas-por-periodo&formato=pdf&desde=2026-10-01"
                    + "&hasta=2026-10-31&agrupacion=mes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Archivo generado"),
            @ApiResponse(responseCode = "400", description = "Reporte, formato o filtros inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "No hay una sesión iniciada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<byte[]> exportar(
            @Parameter(description = "Id del reporte", example = "ventas-por-periodo")
            String reporte,
            @Parameter(description = "Formato: xlsx, csv o pdf", example = "pdf")
            String formato,
            @Parameter(description = "Filtros del reporte")
            Map<String, String> parametros);
}

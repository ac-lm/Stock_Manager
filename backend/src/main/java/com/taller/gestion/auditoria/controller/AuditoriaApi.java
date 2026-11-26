package com.taller.gestion.auditoria.controller;

import com.taller.gestion.auditoria.dto.AuditoriaCreateDTO;
import com.taller.gestion.auditoria.dto.AuditoriaResponseDTO;
import com.taller.gestion.auditoria.entity.AccionAuditoria;
import com.taller.gestion.common.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

@Tag(name = "Auditoría",
        description = "Consulta del historial de operaciones, alta manual de eventos de negocio "
                + "y correcciones hacia adelante. Requiere una sesión iniciada con rol "
                + "ADMINISTRADOR. Las filas no se editan ni se borran: son evidencia.")
public interface AuditoriaApi {

    @Operation(summary = "Buscar registros de auditoría",
            description = "Filtros opcionales por usuario, acción, tabla y rango de fechas. "
                    + "Se ordena por fecha y hora descendente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de registros"),
            @ApiResponse(responseCode = "401", description = "No hay una sesión iniciada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    Page<AuditoriaResponseDTO> listar(
            @Parameter(description = "Id del usuario que realizó la operación") Long id_usuario,
            @Parameter(description = "Acción registrada") AccionAuditoria accion,
            @Parameter(description = "Texto a buscar en el nombre de la tabla") String tabla,
            @Parameter(description = "Desde (fecha y hora ISO)") LocalDateTime desde,
            @Parameter(description = "Hasta (fecha y hora ISO)") LocalDateTime hasta,
            @ParameterObject Pageable pageable);

    @Operation(summary = "Obtener un registro de auditoría por id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado"),
            @ApiResponse(responseCode = "401", description = "No hay una sesión iniciada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe ese registro",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    AuditoriaResponseDTO obtener(
            @Parameter(description = "Id del registro de auditoría", example = "1") Long id_auditoria);

    @Operation(summary = "Registrar una operación o corregir una carga manual",
            description = "Alta manual para acciones que no se deducen de un cambio de entidad. "
                    + "Con accion CORRECCION_AUDITORIA e id_auditoria_corregida se agrega una "
                    + "nueva fila que enmienda a la original, sin modificarla ni borrarla. "
                    + "La fecha y el usuario se toman de la sesión.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Operación registrada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "No hay una sesión iniciada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe la auditoría a corregir",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<AuditoriaResponseDTO> crear(AuditoriaCreateDTO dto);
}

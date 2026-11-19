package com.taller.gestion.costos.controller;

import com.taller.gestion.common.exception.ErrorResponse;
import com.taller.gestion.costos.dto.EstadoDTO;
import com.taller.gestion.costos.dto.TipoCostoCreateDTO;
import com.taller.gestion.costos.dto.TipoCostoResponseDTO;
import com.taller.gestion.costos.dto.TipoCostoUpdateDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Tipos de costo",
        description = "Alta, edición y activación/desactivación de los tipos de costo. "
                + "El tipo reservado para insumos del sistema no se puede modificar ni desactivar. "
                + "Requiere una sesión iniciada con rol ADMINISTRADOR.")
public interface TipoCostoApi {

    @Operation(summary = "Listar tipos de costo",
            description = "Por defecto devuelve solo los activos, ordenados por nombre.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de tipos de costo"),
            @ApiResponse(responseCode = "401", description = "No hay una sesión iniciada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    List<TipoCostoResponseDTO> listar(
            @Parameter(description = "Si es true, incluye también los tipos desactivados")
            boolean incluirInactivos);

    @Operation(summary = "Registrar un tipo de costo",
            description = "El nombre se normaliza para evitar duplicados por mayúsculas, acentos "
                    + "o espacios. El tipo se crea activo y no reservado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tipo de costo creado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "No hay una sesión iniciada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe un tipo con ese nombre",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<TipoCostoResponseDTO> crear(TipoCostoCreateDTO dto);

    @Operation(summary = "Modificar un tipo de costo",
            description = "Actualiza el nombre. El tipo reservado del sistema no se puede modificar.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tipo de costo modificado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "No hay una sesión iniciada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe un tipo con ese id",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Nombre duplicado o tipo reservado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    TipoCostoResponseDTO actualizar(
            @Parameter(description = "Id del tipo de costo", example = "1") Long id_tipo_costo,
            TipoCostoUpdateDTO dto);

    @Operation(summary = "Activar o desactivar un tipo de costo",
            description = "Los tipos desactivados no se pueden usar al cargar costos manuales. "
                    + "El tipo reservado del sistema no cambia de estado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado"),
            @ApiResponse(responseCode = "400", description = "Falta el campo 'activo'",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "No hay una sesión iniciada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe un tipo con ese id",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Es el tipo reservado del sistema",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    TipoCostoResponseDTO cambiarEstado(
            @Parameter(description = "Id del tipo de costo", example = "1") Long id_tipo_costo,
            EstadoDTO dto);
}

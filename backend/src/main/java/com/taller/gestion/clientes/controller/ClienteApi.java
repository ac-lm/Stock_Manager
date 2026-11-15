package com.taller.gestion.clientes.controller;

import com.taller.gestion.clientes.dto.*;
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

import java.util.List;

@Tag(name = "Clientes",
        description = "Alta, búsqueda, edición y baja lógica de clientes. "
                + "El cliente genérico no se puede modificar ni desactivar.")
public interface ClienteApi {

    @Operation(summary = "Buscar clientes",
            description = "Busca por nombre o razón social y por CUIT (con o sin guiones). "
                    + "Por defecto devuelve solo los clientes activos.")
    @ApiResponse(responseCode = "200", description = "Página de clientes")
    Page<ClienteResponseDTO> listar(
            @Parameter(description = "Texto a buscar en el nombre o en el CUIT", example = "panaderia")
            String busqueda,
            @Parameter(description = "Si es true, incluye también los clientes inactivos")
            boolean incluirInactivos,
            @ParameterObject Pageable pageable);

    @Operation(summary = "Obtener un cliente por id",
            description = "Devuelve el cliente aunque esté inactivo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
            @ApiResponse(responseCode = "404", description = "No existe un cliente con ese id",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ClienteResponseDTO obtener(@Parameter(description = "Id del cliente", example = "1") Long id);

    @Operation(summary = "Registrar un cliente",
            description = "El CUIT es obligatorio para grandes clientes y opcional para el resto. "
                    + "El plazo de pago solo se admite para grandes clientes. "
                    + "El tipo GENERICO está reservado para el sistema.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cliente creado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o regla de negocio incumplida",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe un cliente con ese CUIT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<ClienteResponseDTO> crear(ClienteCreateDTO dto);

    @Operation(summary = "Modificar un cliente",
            description = "Aplica las mismas reglas que el alta. Si el cliente deja de ser gran cliente, "
                    + "se eliminan sus condiciones comerciales. No se puede modificar al cliente genérico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente modificado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe un cliente con ese id",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "CUIT duplicado o cliente genérico",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ClienteResponseDTO actualizar(@Parameter(description = "Id del cliente", example = "1") Long id,
                                  ClienteUpdateDTO dto);

    @Operation(summary = "Desactivar o reactivar un cliente",
            description = "Baja lógica: el cliente no se borra, conserva su historial y deja de "
                    + "aparecer al vender. No se puede desactivar al cliente genérico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado"),
            @ApiResponse(responseCode = "400", description = "Falta el campo 'activo'",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe un cliente con ese id",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Es el cliente genérico",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ClienteResponseDTO cambiarEstado(@Parameter(description = "Id del cliente", example = "1") Long id,
                                     ClienteEstadoDTO dto);

    @Operation(summary = "Buscar clientes con nombre parecido",
            description = "Para avisar al registrar un cliente si ya existe uno similar. No distingue "
                    + "mayúsculas, acentos ni espacios de más. Incluye inactivos, excluye al genérico "
                    + "y no devuelve resultados con menos de 3 letras.")
    @ApiResponse(responseCode = "200", description = "Lista de hasta 5 clientes parecidos")
    List<ClienteSimilarDTO> similares(
            @Parameter(description = "Nombre que se está escribiendo", example = "Panaderia Sol")
            String nombre);
}
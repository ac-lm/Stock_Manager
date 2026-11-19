package com.taller.gestion.costos.controller;

import com.taller.gestion.common.exception.ErrorResponse;
import com.taller.gestion.costos.dto.CostoAnularDTO;
import com.taller.gestion.costos.dto.CostoCreateDTO;
import com.taller.gestion.costos.dto.CostoResponseDTO;
import com.taller.gestion.costos.dto.CostoUpdateDTO;
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

import java.math.BigDecimal;

@Tag(name = "Costos",
        description = "Altas, consultas, edición y anulación de costos. "
                + "Los costos de insumos se generan desde el ingreso de mercadería y no se cargan a mano. "
                + "El usuario de alta y el de anulación se toman de la sesión actual. "
                + "Requiere una sesión iniciada con rol ADMINISTRADOR.")
public interface CostoApi {

    @Operation(summary = "Buscar costos",
            description = "Filtra por tipo, mes y año. Por defecto excluye los anulados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de costos"),
            @ApiResponse(responseCode = "401", description = "No hay una sesión iniciada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    Page<CostoResponseDTO> listar(
            @Parameter(description = "Id del tipo de costo", example = "1")
            Long id_tipo_costo,
            @Parameter(description = "Mes del período (1 a 12)", example = "11")
            Integer mes,
            @Parameter(description = "Año del período", example = "2026")
            Integer anio,
            @Parameter(description = "Si es true, incluye también los costos anulados")
            boolean incluirAnulados,
            @ParameterObject Pageable pageable);

    @Operation(summary = "Total de costos",
            description = "Suma los montos de los costos que cumplen el filtro. "
                    + "Nunca incluye anulados, aunque se estén mostrando en la búsqueda.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Total de los costos del filtro"),
            @ApiResponse(responseCode = "401", description = "No hay una sesión iniciada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    BigDecimal total(
            @Parameter(description = "Id del tipo de costo", example = "1") Long id_tipo_costo,
            @Parameter(description = "Mes del período (1 a 12)", example = "11") Integer mes,
            @Parameter(description = "Año del período", example = "2026") Integer anio);

    @Operation(summary = "Obtener un costo por id",
            description = "Devuelve el costo incluso si está anulado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Costo encontrado"),
            @ApiResponse(responseCode = "401", description = "No hay una sesión iniciada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe un costo con ese id",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    CostoResponseDTO obtener(
            @Parameter(description = "Id del costo", example = "10") Long id_costo);

    @Operation(summary = "Registrar un costo",
            description = "El tipo tiene que existir, estar activo y no ser reservado. "
                    + "El usuario que carga el costo se toma de la sesión actual, no del cuerpo.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Costo creado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos: el tipo no existe, está desactivado o es reservado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "No hay una sesión iniciada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<CostoResponseDTO> crear(CostoCreateDTO dto);

    @Operation(summary = "Modificar un costo",
            description = "No se puede modificar un costo anulado ni uno generado por un ingreso "
                    + "de mercadería. El usuario de alta no cambia.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Costo modificado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos: el tipo no existe, está desactivado o es reservado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "No hay una sesión iniciada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe el costo indicado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "El costo está anulado o viene de un ingreso",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    CostoResponseDTO actualizar(
            @Parameter(description = "Id del costo", example = "10") Long id_costo,
            CostoUpdateDTO dto);

    @Operation(summary = "Anular un costo",
            description = "Baja lógica: conserva el costo y registra motivo, fecha y usuario de la "
                    + "anulación. No se puede anular un costo ya anulado ni uno generado por un ingreso.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Costo anulado"),
            @ApiResponse(responseCode = "400", description = "Falta el motivo",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "No hay una sesión iniciada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe un costo con ese id",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "El costo ya está anulado o viene de un ingreso",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    CostoResponseDTO anular(
            @Parameter(description = "Id del costo", example = "10") Long id_costo,
            CostoAnularDTO dto);
}

package com.taller.gestion.caja.controller;

import com.taller.gestion.caja.dto.AbrirCajaDTO;
import com.taller.gestion.caja.dto.CajaActualDTO;
import com.taller.gestion.caja.dto.CerrarCajaDTO;
import com.taller.gestion.caja.dto.MovimientoCajaCreateDTO;
import com.taller.gestion.caja.dto.MovimientoCajaResponseDTO;
import com.taller.gestion.caja.dto.RevisionCajaDTO;
import com.taller.gestion.caja.dto.SesionCajaDetalleDTO;
import com.taller.gestion.caja.dto.SesionCajaResponseDTO;
import com.taller.gestion.caja.entity.EstadoSesionCaja;
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

import java.time.LocalDate;

@Tag(name = "Caja",
        description = "Apertura, egresos, cierre, revisión e historial de las sesiones de caja. "
                + "Hay una sola caja: nunca puede haber dos sesiones abiertas al mismo tiempo. "
                + "Los vendedores operan y consultan; solo un administrador revisa las sesiones "
                + "con diferencia.")
public interface CajaApi {

    @Operation(summary = "Ver la caja actual",
            description = "Devuelve la sesión abierta con sus movimientos y el monto inicial "
                    + "sugerido para abrir una nueva. Si no hay sesión abierta, 'sesion' es null.")
    @ApiResponse(responseCode = "200", description = "Estado actual de la caja")
    CajaActualDTO actual();

    @Operation(summary = "Abrir una sesión de caja",
            description = "El usuario se toma de la sesión actual. Si el monto difiere del "
                    + "sugerido, el motivo es obligatorio. No se puede abrir si ya hay una "
                    + "sesión ABIERTA.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sesión abierta"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o falta el motivo",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ya hay una sesión abierta",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<SesionCajaResponseDTO> abrir(AbrirCajaDTO dto);

    @Operation(summary = "Registrar un egreso",
            description = "Registra un RETIRO o GASTO en una sesión ABIERTA. Un egreso en "
                    + "efectivo no puede superar el efectivo esperado en ese momento. Los "
                    + "egresos por transferencia no tienen ese límite.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Egreso registrado"),
            @ApiResponse(responseCode = "400", description = "Concepto o monto inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe la sesión",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409",
                    description = "La sesión no está abierta o el efectivo no alcanza",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<MovimientoCajaResponseDTO> registrarEgreso(
            @Parameter(description = "Id de la sesión de caja", example = "1") Long id_sesion,
            MovimientoCajaCreateDTO dto);

    @Operation(summary = "Cerrar la caja",
            description = "Declara lo que hay en efectivo y el neto por transferencia "
                    + "(cobros menos pagos, puede ser negativo). Crea el detalle por medio, "
                    + "guarda los totales y pasa a CERRADA o PENDIENTE_REVISION. "
                    + "La observación es obligatoria si alguna diferencia no es cero.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesión cerrada"),
            @ApiResponse(responseCode = "400",
                    description = "Datos inválidos o falta la observación con diferencia",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe la sesión",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "La sesión ya está cerrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    SesionCajaResponseDTO cerrar(
            @Parameter(description = "Id de la sesión de caja", example = "1") Long id_sesion,
            CerrarCajaDTO dto);

    @Operation(summary = "Revisar una sesión con diferencia",
            description = "Solo administradores. La sesión debe estar PENDIENTE_REVISION. "
                    + "Guarda quién, cuándo y la observación, y pasa a CERRADA sin cambiar montos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesión revisada"),
            @ApiResponse(responseCode = "400", description = "Falta la observación",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe la sesión",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409",
                    description = "La sesión no está pendiente de revisión",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    SesionCajaResponseDTO revisar(
            @Parameter(description = "Id de la sesión de caja", example = "1") Long id_sesion,
            RevisionCajaDTO dto);

    @Operation(summary = "Historial de sesiones",
            description = "Filtra por rango de fechas de apertura, estado y usuario. El filtro "
                    + "por usuario incluye tanto al que abrió como al que cerró.")
    @ApiResponse(responseCode = "200", description = "Página de sesiones")
    Page<SesionCajaResponseDTO> listar(
            @Parameter(description = "Estado de la sesión") EstadoSesionCaja estado,
            @Parameter(description = "Id del usuario que abrió o cerró", example = "5")
            Long id_usuario,
            @Parameter(description = "Fecha de apertura desde (AAAA-MM-DD)", example = "2026-11-01")
            LocalDate desde,
            @Parameter(description = "Fecha de apertura hasta (AAAA-MM-DD)", example = "2026-11-30")
            LocalDate hasta,
            @ParameterObject Pageable pageable);

    @Operation(summary = "Detalle de una sesión",
            description = "Devuelve la sesión, sus totales por medio y todos sus movimientos, "
                    + "del más nuevo al más viejo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalle de la sesión"),
            @ApiResponse(responseCode = "404", description = "No existe la sesión",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    SesionCajaDetalleDTO obtener(
            @Parameter(description = "Id de la sesión de caja", example = "1") Long id_sesion);
}

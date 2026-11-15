package com.taller.gestion.usuario.controller;

import com.taller.gestion.common.exception.ErrorResponse;
import com.taller.gestion.usuario.dto.UsuarioCreateDTO;
import com.taller.gestion.usuario.dto.UsuarioEstadoDTO;
import com.taller.gestion.usuario.dto.UsuarioPasswordDTO;
import com.taller.gestion.usuario.dto.UsuarioResponseDTO;
import com.taller.gestion.usuario.dto.UsuarioUpdateDTO;
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

@Tag(name = "Usuarios",
        description = "Alta, búsqueda, edición, baja lógica y cambio de contraseña de usuarios. "
                + "Requiere una sesión iniciada con rol ADMINISTRADOR.")
public interface UsuarioApi {

    @Operation(summary = "Buscar usuarios",
            description = "Busca por nombre o email. Por defecto devuelve solo los usuarios activos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de usuarios"),
            @ApiResponse(responseCode = "401", description = "No hay una sesión iniciada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    Page<UsuarioResponseDTO> listar(
            @Parameter(description = "Texto a buscar en el nombre o en el email", example = "ana")
            String busqueda,
            @Parameter(description = "Si es true, incluye también los usuarios inactivos")
            boolean incluirInactivos,
            @ParameterObject Pageable pageable);

    @Operation(summary = "Obtener un usuario por id",
            description = "Devuelve el usuario aunque esté inactivo. Nunca incluye la contraseña.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
            @ApiResponse(responseCode = "401", description = "No hay una sesión iniciada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe un usuario con ese id",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    UsuarioResponseDTO obtener(
            @Parameter(description = "Id del usuario", example = "1") Long id_usuario);

    @Operation(summary = "Registrar un usuario",
            description = "El email debe ser único y la contraseña se guarda cifrada con BCrypt. "
                    + "El usuario se crea activo.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario creado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "No hay una sesión iniciada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe un usuario con ese email",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<UsuarioResponseDTO> crear(UsuarioCreateDTO dto);

    @Operation(summary = "Modificar un usuario",
            description = "Actualización parcial: los campos nulos no se modifican. "
                    + "No permite dejar el sistema sin ningún administrador activo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario modificado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "No hay una sesión iniciada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe un usuario con ese id",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email duplicado o último administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    UsuarioResponseDTO actualizar(
            @Parameter(description = "Id del usuario", example = "1") Long id_usuario,
            UsuarioUpdateDTO dto);

    @Operation(summary = "Activar o desactivar un usuario",
            description = "Baja lógica: el usuario no se borra y conserva su historial, pero "
                    + "no puede iniciar sesión. No permite desactivar el usuario de la sesión "
                    + "actual ni dejar el sistema sin administradores activos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado"),
            @ApiResponse(responseCode = "400", description = "Falta el campo 'activo'",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "No hay una sesión iniciada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe un usuario con ese id",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Es el usuario actual o el último administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    UsuarioResponseDTO cambiarEstado(
            @Parameter(description = "Id del usuario", example = "1") Long id_usuario,
            UsuarioEstadoDTO dto);

    @Operation(summary = "Asignar una nueva contraseña",
            description = "Cambio administrativo de contraseña. Se guarda cifrada con BCrypt.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Contraseña actualizada"),
            @ApiResponse(responseCode = "400", description = "Contraseña inválida",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "No hay una sesión iniciada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe un usuario con ese id",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Void> cambiarPassword(
            @Parameter(description = "Id del usuario", example = "1") Long id_usuario,
            UsuarioPasswordDTO dto);
}

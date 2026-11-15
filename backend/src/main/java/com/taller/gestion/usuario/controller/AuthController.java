package com.taller.gestion.usuario.controller;

import com.taller.gestion.common.exception.ErrorResponse;
import com.taller.gestion.usuario.dto.LoginRequest;
import com.taller.gestion.usuario.dto.UsuarioResponseDTO;
import com.taller.gestion.usuario.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación",
        description = "Login y logout con sesión HTTP. El login devuelve una cookie de sesión "
                + "que el navegador reenvía automáticamente en las siguientes peticiones.")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UsuarioService usuarioService;
    private final SecurityContextRepository securityContextRepository;
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    @Operation(summary = "Iniciar sesión",
            description = "Valida el email y la contraseña, crea la sesión y devuelve los datos "
                    + "del usuario. Un usuario inactivo no puede iniciar sesión.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesión iniciada"),
            @ApiResponse(responseCode = "400", description = "Faltan datos o el email es inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credenciales incorrectas o cuenta deshabilitada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public UsuarioResponseDTO login(@Valid @RequestBody LoginRequest dto,
                                    HttpServletRequest request,
                                    HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.email(), dto.password()));

        // Protección contra fijación de sesión: si ya había una sesión, se renueva el id.
        HttpSession sesionExistente = request.getSession(false);
        if (sesionExistente != null) {
            request.changeSessionId();
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        return usuarioService.buscarPorEmail(authentication.getName());
    }

    @Operation(summary = "Obtener el usuario de la sesión actual",
            description = "Devuelve los datos del usuario autenticado. Sirve para restaurar la "
                    + "sesión en el frontend al recargar la página.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario autenticado"),
            @ApiResponse(responseCode = "401", description = "No hay una sesión iniciada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/me")
    public UsuarioResponseDTO me(Authentication authentication) {
        return usuarioService.buscarPorEmail(authentication.getName());
    }

    @Operation(summary = "Cerrar sesión",
            description = "Invalida la sesión actual y borra la cookie del navegador.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sesión cerrada"),
            @ApiResponse(responseCode = "401", description = "No hay una sesión iniciada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        logoutHandler.logout(request, response,
                SecurityContextHolder.getContext().getAuthentication());
        return ResponseEntity.noContent().build();
    }
}

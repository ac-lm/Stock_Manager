package com.taller.gestion.common.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DatosInvalidosException.class)
    public ResponseEntity<ErrorResponse> handleDatosInvalidos(DatosInvalidosException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleNoEncontrado(RecursoNoEncontradoException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<ErrorResponse> handleDuplicado(RecursoDuplicadoException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(OperacionNoPermitidaException.class)
    public ResponseEntity<ErrorResponse> handleOperacionNoPermitida(OperacionNoPermitidaException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    // Credenciales inválidas o cuenta deshabilitada en el login.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        String mensaje = ex instanceof DisabledException
                ? "La cuenta está deshabilitada"
                : ex instanceof AuthenticationCredentialsNotFoundException
                        ? ex.getMessage()
                        : "Email o contraseña incorrectos";
        return build(HttpStatus.UNAUTHORIZED, mensaje, null);
    }

    // Errores de @Valid en los DTOs (@NotBlank, @Email, @Pattern, etc.)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidacion(MethodArgumentNotValidException ex) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Hay errores de validación", detalles);
    }

    // Errores de @Validated en parámetros de métodos
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> detalles = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Hay errores de validación", detalles);
    }

    // Body JSON ausente o mal formado
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMensajeNoLegible(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud es inválido", null);
    }

    // Parámetros con tipo incorrecto (por ejemplo un ID no numérico)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTipoIncorrecto(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST,
                "El parámetro '" + ex.getName() + "' tiene un formato inválido", null);
    }

    // Restricciones de la base de datos (unique, foreign key, etc.)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegridad(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad de datos", ex);
        return build(HttpStatus.CONFLICT,
                "La operación entra en conflicto con datos existentes", null);
    }

    // Transitorio: normaliza IllegalArgumentException como error de datos
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleArgumentoInvalido(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    // Errores lanzados con ResponseStatusException conservan su estado
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String mensaje = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        return build(status, mensaje, null);
    }

    // Red de seguridad: cualquier error inesperado
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenerico(Exception ex) {
        log.error("Error inesperado no controlado", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String mensaje, List<String> detalles) {
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(), status.value(), status.getReasonPhrase(), mensaje, detalles);
        return ResponseEntity.status(status).body(body);
    }
}

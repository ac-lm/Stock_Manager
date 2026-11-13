package com.taller.gestion.catalogo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ProductoInsumoDuplicadoException extends RuntimeException {
    public ProductoInsumoDuplicadoException(String message) {
        super(message);
    }
}

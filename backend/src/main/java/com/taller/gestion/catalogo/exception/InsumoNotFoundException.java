package com.taller.gestion.catalogo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class InsumoNotFoundException extends RuntimeException {
    public InsumoNotFoundException(String message) {
        super(message);
    }
}

package com.taller.gestion.reportes.exportacion;

import com.taller.gestion.common.exception.DatosInvalidosException;

import java.time.LocalDate;
import java.util.Map;

/** Lectura común de parámetros de reportes: un valor ausente o inválido es 400. */
public final class ParametrosReporte {

    private ParametrosReporte() {
    }

    public static LocalDate fecha(Map<String, String> parametros, String nombre) {
        String valor = parametros.get(nombre);
        if (valor == null || valor.isBlank()) {
            throw new DatosInvalidosException("El parámetro '" + nombre + "' es obligatorio");
        }
        try {
            return LocalDate.parse(valor);
        } catch (RuntimeException ex) {
            throw new DatosInvalidosException(
                    "El parámetro '" + nombre + "' tiene un formato inválido");
        }
    }

    public static String texto(Map<String, String> parametros, String nombre, String defecto) {
        String valor = parametros.get(nombre);
        return valor == null || valor.isBlank() ? defecto : valor;
    }
}

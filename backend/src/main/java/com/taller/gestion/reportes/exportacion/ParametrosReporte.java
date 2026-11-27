package com.taller.gestion.reportes.exportacion;

import com.taller.gestion.common.exception.DatosInvalidosException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Map;

/** Lectura común de parámetros de reportes: un valor ausente o inválido es 400. */
public final class ParametrosReporte {

    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("uuuu-MM")
            .withResolverStyle(ResolverStyle.STRICT);

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

    /**
     * Período {@code YYYY-MM}. La variante con texto devuelve null cuando falta para que el
     * controller pueda delegar el mensaje de "rango obligatorio" en el service, igual que en
     * los reportes con rango de fechas.
     */
    public static YearMonth mes(String valor, String nombre) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return YearMonth.parse(valor.trim(), MES);
        } catch (RuntimeException ex) {
            throw new DatosInvalidosException(
                    "El parámetro '" + nombre + "' tiene un formato inválido");
        }
    }

    public static YearMonth mes(Map<String, String> parametros, String nombre) {
        YearMonth periodo = mes(parametros.get(nombre), nombre);
        if (periodo == null) {
            throw new DatosInvalidosException("El parámetro '" + nombre + "' es obligatorio");
        }
        return periodo;
    }

    public static String texto(Map<String, String> parametros, String nombre, String defecto) {
        String valor = parametros.get(nombre);
        return valor == null || valor.isBlank() ? defecto : valor;
    }
}

package com.taller.gestion.reportes.dto;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * Formas de agrupar el período de un reporte. La semana empieza el lunes, igual que
 * {@code date_trunc('week', ...)} en PostgreSQL.
 */
public enum AgrupacionPeriodo {
    DIA,
    SEMANA,
    MES;

    /** La respuesta usa las mismas palabras que se envían como parámetro: dia, semana, mes. */
    @JsonValue
    public String valor() {
        return name().toLowerCase(Locale.ROOT);
    }
}

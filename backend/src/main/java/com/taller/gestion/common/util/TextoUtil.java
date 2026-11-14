package com.taller.gestion.common.util;

public final class TextoUtil {
    private TextoUtil() {}

    /** Saca espacios de los bordes y junta espacios repetidos. Vacío pasa a null. */
    public static String limpiar(String texto) {
        if (texto == null) return null;
        String resultado = texto.trim().replaceAll("\\s+", " ");
        return resultado.isEmpty() ? null : resultado;
    }

    /** Igual que limpiar, pero además pasa a minúsculas (para emails). */
    public static String limpiarEmail(String email) {
        String resultado = limpiar(email);
        return resultado == null ? null : resultado.toLowerCase();
    }
}
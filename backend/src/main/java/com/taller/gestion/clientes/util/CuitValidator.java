package com.taller.gestion.clientes.util;

public final class CuitValidator {
    private static final int[] PESOS = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};

    private CuitValidator() {}

    public static boolean esValido(String cuit) {
        String d = cuit.replaceAll("\\D", "");
        if (d.length() != 11) return false;

        int suma = 0;
        for (int i = 0; i < 10; i++) {
            suma += Character.getNumericValue(d.charAt(i)) * PESOS[i];
        }
        int verificador = 11 - (suma % 11);
        if (verificador == 11) verificador = 0;
        if (verificador == 10) verificador = 9;

        return verificador == Character.getNumericValue(d.charAt(10));
    }
}
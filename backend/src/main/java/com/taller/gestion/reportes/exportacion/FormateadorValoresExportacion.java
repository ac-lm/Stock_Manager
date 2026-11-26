package com.taller.gestion.reportes.exportacion;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Representación de cada valor en los archivos de texto: coma decimal en es-AR. */
@Component
public class FormateadorValoresExportacion {

    private static final Locale LOCALE = Locale.forLanguageTag("es-AR");

    public String formatear(Object valor, TipoDatoExportable tipo) {
        if (valor == null) {
            return "";
        }
        return switch (tipo) {
            case DECIMAL -> new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(LOCALE))
                    .format(new BigDecimal(valor.toString()));
            case ENTERO -> String.valueOf(((Number) valor).longValue());
            case TEXTO -> valor.toString();
        };
    }
}

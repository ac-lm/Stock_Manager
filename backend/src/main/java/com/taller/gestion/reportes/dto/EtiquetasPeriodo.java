package com.taller.gestion.reportes.dto;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Único lugar donde se decide cómo se muestra un período en pantalla y en las exportaciones. */
public final class EtiquetasPeriodo {

    private static final Locale LOCALE = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MMM", LOCALE);

    private EtiquetasPeriodo() {
    }

    public static String de(LocalDate inicio, AgrupacionPeriodo tipo) {
        return switch (tipo) {
            case DIA -> inicio.toString();
            case SEMANA -> "Semana del " + inicio;
            case MES -> mes(inicio);
        };
    }

    /** CLDR escribe "oct."; en el reporte queremos "oct 2026". */
    private static String mes(LocalDate inicio) {
        YearMonth mes = YearMonth.from(inicio);
        return MES.format(mes).replace(".", "") + " " + mes.getYear();
    }
}

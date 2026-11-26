package com.taller.gestion.reportes.exportacion;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** La fecha de emisión se muestra siempre en hora local del taller, sin sufijo de zona. */
@Component
public class FormateadorFechaEmision {

    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-AR"));

    private final Clock clock;

    public FormateadorFechaEmision(Clock clock) {
        this.clock = clock;
    }

    public String formatear() {
        return LocalDateTime.now(clock).format(FORMATO);
    }
}

package com.taller.gestion.reportes.exportacion;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ReportesExportacionConfig {

    /** Zona horaria del taller: la misma que usa la pantalla para "hoy". */
    public static final ZoneId ZONA_TALLER = ZoneId.of("America/Argentina/Buenos_Aires");

    @Bean
    public Clock relojReportes() {
        return Clock.system(ZONA_TALLER);
    }
}

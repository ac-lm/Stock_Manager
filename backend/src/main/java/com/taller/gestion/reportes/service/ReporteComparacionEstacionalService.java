package com.taller.gestion.reportes.service;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.ComparacionEstacionalFilaDTO;
import com.taller.gestion.reportes.dto.ReporteComparacionEstacionalDTO;
import com.taller.gestion.reportes.dto.TotalesComparacionEstacionalDTO;
import com.taller.gestion.reportes.repository.ReporteComparacionEstacionalRepository;
import com.taller.gestion.reportes.repository.ResumenVentasPorAnioMes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Evolución mensual de las ventas y comparación del mismo mes entre años. */
@Service
@RequiredArgsConstructor
public class ReporteComparacionEstacionalService {

    public static final int MAX_ANIOS = 5;

    private static final int ANIO_MINIMO = 2000;
    private static final Locale LOCALE = Locale.forLanguageTag("es-AR");
    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final ReporteComparacionEstacionalRepository reporteComparacionEstacionalRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ReporteComparacionEstacionalDTO comparacionEstacional(
            String aniosTexto, Long idProducto, String categoria) {

        List<Integer> anios = parsearAnios(aniosTexto);
        String categoriaFiltrada = categoria == null || categoria.isBlank() ? null : categoria.trim();

        LocalDate hoy = LocalDate.now(clock);
        int anioActual = hoy.getYear();
        int ultimoAnio = anios.get(anios.size() - 1);
        int ultimoMes = ultimoAnio == anioActual ? hoy.getMonthValue() : 12;

        LocalDate desde = LocalDate.of(anios.get(0), 1, 1);
        LocalDate hastaExclusivo = LocalDate.of(ultimoAnio + 1, 1, 1);

        Map<AnioMes, BigDecimal> porMes = new HashMap<>();
        for (ResumenVentasPorAnioMes resumen : reporteComparacionEstacionalRepository.consultar(
                desde, hastaExclusivo, idProducto, categoriaFiltrada)) {
            porMes.put(new AnioMes(resumen.anio(), resumen.mes()),
                    resumen.totalVendido().setScale(2, RoundingMode.HALF_UP));
        }

        Map<String, BigDecimal> totalesPorAnio = new HashMap<>();
        for (int anio : anios) {
            totalesPorAnio.put(clave(anio), CERO);
        }

        List<ComparacionEstacionalFilaDTO> filas = new ArrayList<>();
        for (int mes = 1; mes <= ultimoMes; mes++) {
            Map<String, BigDecimal> totalesFila = new HashMap<>();
            Map<String, BigDecimal> variacionesFila = new HashMap<>();

            for (int anio : anios) {
                BigDecimal total = porMes.getOrDefault(new AnioMes(anio, mes), CERO);
                totalesFila.put(clave(anio), total);
                totalesPorAnio.merge(clave(anio), total, BigDecimal::add);
            }
            for (int i = 1; i < anios.size(); i++) {
                BigDecimal variacion = variacion(
                        totalesFila.get(clave(anios.get(i))),
                        totalesFila.get(clave(anios.get(i - 1))));
                if (variacion != null) {
                    variacionesFila.put(clave(anios.get(i)), variacion);
                }
            }

            filas.add(new ComparacionEstacionalFilaDTO(
                    mes, etiquetaMes(mes), Map.copyOf(totalesFila), Map.copyOf(variacionesFila)));
        }

        Map<String, BigDecimal> variacionesAnuales = new HashMap<>();
        for (int i = 1; i < anios.size(); i++) {
            BigDecimal variacion = variacion(
                    totalesPorAnio.get(clave(anios.get(i))),
                    totalesPorAnio.get(clave(anios.get(i - 1))));
            if (variacion != null) {
                variacionesAnuales.put(clave(anios.get(i)), variacion);
            }
        }

        return new ReporteComparacionEstacionalDTO(
                anios, idProducto, categoriaFiltrada, desde,
                YearMonth.of(ultimoAnio, ultimoMes).atEndOfMonth(),
                List.copyOf(filas),
                new TotalesComparacionEstacionalDTO(
                        Map.copyOf(totalesPorAnio), Map.copyOf(variacionesAnuales)));
    }

    /** Ausentes: el año actual y los dos anteriores. Presentes: hasta 5 años, sin repetir. */
    private List<Integer> parsearAnios(String valor) {
        int anioActual = LocalDate.now(clock).getYear();
        if (valor == null || valor.isBlank()) {
            return List.of(anioActual - 2, anioActual - 1, anioActual);
        }

        List<Integer> anios;
        try {
            anios = Arrays.stream(valor.split(","))
                    .map(String::trim)
                    .filter(parte -> !parte.isEmpty())
                    .map(Integer::parseInt)
                    .toList();
        } catch (NumberFormatException ex) {
            throw new DatosInvalidosException("El parámetro 'anios' tiene un formato inválido");
        }

        if (anios.isEmpty()) {
            throw new DatosInvalidosException("Debe elegir al menos un año para comparar");
        }
        if (anios.size() > MAX_ANIOS) {
            throw new DatosInvalidosException("Se pueden comparar hasta 5 años");
        }
        if (anios.stream().distinct().count() != anios.size()) {
            throw new DatosInvalidosException("Los años a comparar no se pueden repetir");
        }
        if (anios.stream().anyMatch(anio -> anio < ANIO_MINIMO || anio > anioActual)) {
            throw new DatosInvalidosException(
                    "Los años deben estar entre " + ANIO_MINIMO + " y " + anioActual);
        }
        return anios.stream().sorted().toList();
    }

    /** Si el año anterior vendió 0, la variación no tiene sentido y la pantalla muestra "—". */
    private BigDecimal variacion(BigDecimal actual, BigDecimal anterior) {
        if (anterior == null || anterior.compareTo(CERO) == 0) {
            return null;
        }
        return actual.subtract(anterior)
                .divide(anterior, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String clave(int anio) {
        return String.valueOf(anio);
    }

    private String etiquetaMes(int mes) {
        return Month.of(mes).getDisplayName(TextStyle.SHORT, LOCALE).replace(".", "");
    }

    private record AnioMes(int anio, int mes) {
    }
}

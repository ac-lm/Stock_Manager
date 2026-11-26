package com.taller.gestion.reportes.service;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.AgrupacionPeriodo;
import com.taller.gestion.reportes.dto.EtiquetasPeriodo;
import com.taller.gestion.reportes.dto.ReporteVentasPorPeriodoDTO;
import com.taller.gestion.reportes.dto.TotalesVentasPorPeriodoDTO;
import com.taller.gestion.reportes.dto.VentasPorPeriodoFilaDTO;
import com.taller.gestion.reportes.repository.ReporteVentasRepository;
import com.taller.gestion.reportes.repository.ResumenVentasPorPeriodo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReporteVentasService {

    private static final int MAX_ANIOS_DIARIOS = 5;
    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final ReporteVentasRepository reporteVentasRepository;

    @Transactional(readOnly = true)
    public ReporteVentasPorPeriodoDTO ventasPorPeriodo(
            LocalDate desde, LocalDate hasta, String agrupacion) {

        validarRango(desde, hasta);
        AgrupacionPeriodo tipo = parsearAgrupacion(agrupacion);
        if (tipo == AgrupacionPeriodo.DIA && desde.plusYears(MAX_ANIOS_DIARIOS).isBefore(hasta)) {
            throw new DatosInvalidosException(
                    "El rango diario no puede superar 5 años; agrupá por semana o por mes");
        }

        // 'hasta' se incluye completo: el límite superior es el día siguiente.
        List<ResumenVentasPorPeriodo> consulta =
                reporteVentasRepository.consultar(tipo, desde, hasta.plusDays(1));

        Map<LocalDate, ResumenVentasPorPeriodo> porInicio = new HashMap<>();
        for (ResumenVentasPorPeriodo fila : consulta) {
            porInicio.put(fila.inicio(), fila);
        }

        List<VentasPorPeriodoFilaDTO> filas = new ArrayList<>();
        BigDecimal totalVendido = CERO;
        long cantidadVentas = 0;

        // Los períodos sin ventas se completan acá, no en la base de datos.
        for (LocalDate inicio : periodos(desde, hasta, tipo)) {
            ResumenVentasPorPeriodo dato = porInicio.get(inicio);
            BigDecimal total = dato == null ? CERO : dato.totalVendido().setScale(2, RoundingMode.HALF_UP);
            long cantidad = dato == null ? 0L : dato.cantidadVentas();

            filas.add(new VentasPorPeriodoFilaDTO(
                    periodo(inicio, tipo), EtiquetasPeriodo.de(inicio, tipo),
                    total, cantidad, ticketPromedio(total, cantidad)));
            totalVendido = totalVendido.add(total);
            cantidadVentas += cantidad;
        }

        return new ReporteVentasPorPeriodoDTO(
                desde, hasta, tipo, List.copyOf(filas),
                new TotalesVentasPorPeriodoDTO(
                        totalVendido.setScale(2, RoundingMode.HALF_UP),
                        cantidadVentas,
                        ticketPromedio(totalVendido, cantidadVentas)));
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new DatosInvalidosException("El rango de fechas es obligatorio");
        }
        if (desde.isAfter(hasta)) {
            throw new DatosInvalidosException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
    }

    private AgrupacionPeriodo parsearAgrupacion(String agrupacion) {
        if (agrupacion == null || agrupacion.isBlank()) {
            return AgrupacionPeriodo.DIA;
        }
        try {
            return AgrupacionPeriodo.valueOf(agrupacion.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new DatosInvalidosException("La agrupación debe ser dia, semana o mes");
        }
    }

    /** Genera la secuencia completa de períodos: cada día, cada lunes o cada mes del rango. */
    private List<LocalDate> periodos(LocalDate desde, LocalDate hasta, AgrupacionPeriodo tipo) {
        List<LocalDate> resultado = new ArrayList<>();
        switch (tipo) {
            case DIA -> {
                for (LocalDate fecha = desde; !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
                    resultado.add(fecha);
                }
            }
            case SEMANA -> {
                LocalDate fecha = desde.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                for (; !fecha.isAfter(hasta); fecha = fecha.plusWeeks(1)) {
                    resultado.add(fecha);
                }
            }
            case MES -> {
                LocalDate fecha = desde.withDayOfMonth(1);
                for (; !fecha.isAfter(hasta); fecha = fecha.plusMonths(1)) {
                    resultado.add(fecha);
                }
            }
        }
        return resultado;
    }

    private String periodo(LocalDate inicio, AgrupacionPeriodo tipo) {
        return tipo == AgrupacionPeriodo.MES ? YearMonth.from(inicio).toString() : inicio.toString();
    }

    private BigDecimal ticketPromedio(BigDecimal total, long cantidad) {
        if (cantidad == 0) {
            return CERO;
        }
        return total.divide(BigDecimal.valueOf(cantidad), 2, RoundingMode.HALF_UP);
    }
}

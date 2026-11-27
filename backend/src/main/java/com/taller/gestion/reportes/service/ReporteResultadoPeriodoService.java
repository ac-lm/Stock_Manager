package com.taller.gestion.reportes.service;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.AgrupacionPeriodo;
import com.taller.gestion.reportes.dto.CostoPorTipoFilaDTO;
import com.taller.gestion.reportes.dto.ReporteResultadoPeriodoDTO;
import com.taller.gestion.reportes.dto.ReporteVentasPorPeriodoDTO;
import com.taller.gestion.reportes.dto.ResultadoPeriodoFilaDTO;
import com.taller.gestion.reportes.dto.TotalesResultadoPeriodoDTO;
import com.taller.gestion.reportes.dto.VentasPorPeriodoFilaDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cruza los DTOs ya armados por REP‑01 y REP‑10: no consulta ni Venta ni Costo por su cuenta.
 * Ventas van por fecha de venta (agrupación MES) y costos por periodo_anio/periodo_mes.
 */
@Service
@RequiredArgsConstructor
public class ReporteResultadoPeriodoService {

    public static final String ACLARACION =
            "Es un resultado simple, no un balance contable: no considera stock sin vender "
                    + "ni amortizaciones";

    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final ReporteVentasService reporteVentasService;
    private final ReporteCostosPorTipoService reporteCostosPorTipoService;

    @Transactional(readOnly = true)
    public ReporteResultadoPeriodoDTO resultadoPeriodo(YearMonth desde, YearMonth hasta) {
        validarRango(desde, hasta);

        // Mismos servicios que usan las pantallas de REP‑01 y REP‑10, con el mismo rango.
        ReporteVentasPorPeriodoDTO ventas = reporteVentasService.ventasPorPeriodo(
                desde.atDay(1), hasta.atEndOfMonth(), AgrupacionPeriodo.MES.name());
        var costos = reporteCostosPorTipoService.costosPorTipo(desde, hasta, null);

        Map<String, VentasPorPeriodoFilaDTO> ventasPorPeriodo = ventas.filas().stream()
                .collect(Collectors.toMap(VentasPorPeriodoFilaDTO::periodo, Function.identity()));

        // La secuencia de meses es la de REP‑10; REP‑01 genera la misma con desde/hasta de mes.
        List<ResultadoPeriodoFilaDTO> filas = new ArrayList<>();
        BigDecimal totalVendido = CERO;
        BigDecimal totalCostos = CERO;
        for (CostoPorTipoFilaDTO filaCostos : costos.filas()) {
            String periodo = filaCostos.periodo();
            VentasPorPeriodoFilaDTO filaVentas = ventasPorPeriodo.get(periodo);
            BigDecimal vendido = dosDecimales(
                    filaVentas == null ? CERO : filaVentas.totalVendido());
            BigDecimal costo = filaCostos.total();
            filas.add(new ResultadoPeriodoFilaDTO(
                    periodo, filaCostos.etiqueta(), vendido, costo, restar(vendido, costo)));
            totalVendido = sumar(totalVendido, vendido);
            totalCostos = sumar(totalCostos, costo);
        }

        return new ReporteResultadoPeriodoDTO(
                desde.atDay(1), hasta.atEndOfMonth(), desde.toString(), hasta.toString(),
                List.copyOf(filas),
                new TotalesResultadoPeriodoDTO(
                        totalVendido, totalCostos, restar(totalVendido, totalCostos), filas.size()),
                ACLARACION, costos.avisoInsumos());
    }

    private void validarRango(YearMonth desde, YearMonth hasta) {
        if (desde == null || hasta == null) {
            throw new DatosInvalidosException("El rango de períodos es obligatorio");
        }
        if (desde.isAfter(hasta)) {
            throw new DatosInvalidosException("El período 'desde' no puede ser posterior a 'hasta'");
        }
        if (ChronoUnit.MONTHS.between(desde, hasta) + 1
                > ReporteCostosPorTipoService.MAX_MESES) {
            throw new DatosInvalidosException("El rango no puede superar los "
                    + ReporteCostosPorTipoService.MAX_MESES + " meses");
        }
        if (desde.getYear() < ReporteCostosPorTipoService.ANIO_MINIMO
                || hasta.getYear() > ReporteCostosPorTipoService.ANIO_MAXIMO) {
            throw new DatosInvalidosException("Los períodos deben estar entre "
                    + ReporteCostosPorTipoService.ANIO_MINIMO + "-01 y "
                    + ReporteCostosPorTipoService.ANIO_MAXIMO + "-12");
        }
    }

    private BigDecimal dosDecimales(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumar(BigDecimal uno, BigDecimal dos) {
        return uno.add(dos).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal restar(BigDecimal uno, BigDecimal dos) {
        return uno.subtract(dos).setScale(2, RoundingMode.HALF_UP);
    }
}

package com.taller.gestion.reportes.service;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.CostoPorTipoFilaDTO;
import com.taller.gestion.reportes.dto.ReporteCostosPorTipoDTO;
import com.taller.gestion.reportes.dto.TipoCostoOpcionDTO;
import com.taller.gestion.reportes.dto.TipoCostoReporteDTO;
import com.taller.gestion.reportes.dto.TotalesCostosPorTipoDTO;
import com.taller.gestion.reportes.repository.ReporteCostosPorTipoRepository;
import com.taller.gestion.reportes.repository.ResumenCostoPorTipo;
import com.taller.gestion.reportes.repository.TipoCostoDisponible;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Costos por mes y tipo: el período de la fila es periodo_anio/periodo_mes, nunca fecha_pago. */
@Service
@RequiredArgsConstructor
public class ReporteCostosPorTipoService {

    public static final int MAX_MESES = 36;
    public static final int ANIO_MINIMO = 2000;
    public static final int ANIO_MAXIMO = 2100;
    public static final String AVISO_INSUMOS =
            "La evolución de insumos se mostrará cuando se implemente el costo por ingreso "
                    + "de mercadería (CST-06)";

    private static final Locale LOCALE = Locale.forLanguageTag("es-AR");
    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final ReporteCostosPorTipoRepository reporteCostosPorTipoRepository;

    @Transactional(readOnly = true)
    public ReporteCostosPorTipoDTO costosPorTipo(YearMonth desde, YearMonth hasta, Long idTipoCosto) {
        validarRango(desde, hasta);
        int desdeClave = clave(desde);
        int hastaClave = clave(hasta);

        List<TipoCostoDisponible> disponibles =
                reporteCostosPorTipoRepository.tiposDisponibles(desdeClave, hastaClave);
        if (idTipoCosto != null && disponibles.stream()
                .noneMatch(tipo -> tipo.id() == idTipoCosto)) {
            throw new DatosInvalidosException("El tipo de costo no existe");
        }

        List<ResumenCostoPorTipo> resumenes =
                reporteCostosPorTipoRepository.consultar(desdeClave, hastaClave, idTipoCosto);
        Map<String, BigDecimal> totalesPorTipo = new HashMap<>();
        long registros = 0L;
        for (ResumenCostoPorTipo resumen : resumenes) {
            totalesPorTipo.merge(claveTexto(resumen.idTipoCosto()),
                    dosDecimales(resumen.total()), this::sumar);
            registros += resumen.registros();
        }

        // Con "Todos" se muestran los tipos con costos en el rango y siempre el reservado Insumos.
        // Con un tipo elegido, la tabla y el gráfico muestran solo ese tipo (aunque dé 0).
        List<TipoCostoDisponible> visibles = idTipoCosto == null
                ? disponibles.stream()
                        .filter(tipo -> tipo.reservado()
                                || totalesPorTipo.containsKey(claveTexto(tipo.id())))
                        .toList()
                : disponibles.stream().filter(tipo -> tipo.id() == idTipoCosto).toList();

        List<TipoCostoReporteDTO> tipos = visibles.stream()
                .map(tipo -> new TipoCostoReporteDTO(
                        tipo.id(), tipo.nombre(), tipo.reservado(),
                        totalesPorTipo.getOrDefault(claveTexto(tipo.id()), CERO)))
                .toList();

        List<CostoPorTipoFilaDTO> filas = new ArrayList<>();
        BigDecimal totalGeneral = CERO;
        YearMonth mes = desde;
        while (!mes.isAfter(hasta)) {
            final int anio = mes.getYear();
            final int numeroMes = mes.getMonthValue();
            Map<String, BigDecimal> valores = new HashMap<>();
            BigDecimal totalMes = CERO;
            for (TipoCostoReporteDTO tipo : tipos) {
                BigDecimal valor = resumenes.stream()
                        .filter(resumen -> resumen.idTipoCosto() == tipo.id()
                                && resumen.anio() == anio
                                && resumen.mes() == numeroMes)
                        .map(ResumenCostoPorTipo::total)
                        .findFirst()
                        .map(this::dosDecimales)
                        .orElse(CERO);
                valores.put(claveTexto(tipo.id()), valor);
                totalMes = sumar(totalMes, valor);
            }
            filas.add(new CostoPorTipoFilaDTO(
                    mes.toString(), etiquetaMes(mes), Map.copyOf(valores), totalMes));
            totalGeneral = sumar(totalGeneral, totalMes);
            mes = mes.plusMonths(1);
        }

        List<TipoCostoOpcionDTO> opciones = disponibles.stream()
                .map(tipo -> new TipoCostoOpcionDTO(
                        tipo.id(), tipo.nombre(), tipo.activo(), tipo.reservado()))
                .toList();

        return new ReporteCostosPorTipoDTO(
                desde.atDay(1), hasta.atEndOfMonth(), desde.toString(), hasta.toString(),
                idTipoCosto, opciones, tipos, List.copyOf(filas),
                new TotalesCostosPorTipoDTO(
                        totalGeneral, filas.size(), tipos.size(), registros),
                AVISO_INSUMOS);
    }

    private void validarRango(YearMonth desde, YearMonth hasta) {
        if (desde == null || hasta == null) {
            throw new DatosInvalidosException("El rango de períodos es obligatorio");
        }
        if (desde.isAfter(hasta)) {
            throw new DatosInvalidosException("El período 'desde' no puede ser posterior a 'hasta'");
        }
        if (ChronoUnit.MONTHS.between(desde, hasta) + 1 > MAX_MESES) {
            throw new DatosInvalidosException("El rango no puede superar los " + MAX_MESES + " meses");
        }
        if (desde.getYear() < ANIO_MINIMO || hasta.getYear() > ANIO_MAXIMO) {
            throw new DatosInvalidosException("Los períodos deben estar entre "
                    + ANIO_MINIMO + "-01 y " + ANIO_MAXIMO + "-12");
        }
    }

    private int clave(YearMonth periodo) {
        return periodo.getYear() * 100 + periodo.getMonthValue();
    }

    private String claveTexto(long idTipoCosto) {
        return String.valueOf(idTipoCosto);
    }

    private BigDecimal dosDecimales(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumar(BigDecimal uno, BigDecimal dos) {
        return uno.add(dos).setScale(2, RoundingMode.HALF_UP);
    }

    private String etiquetaMes(YearMonth periodo) {
        return Month.of(periodo.getMonthValue())
                .getDisplayName(TextStyle.SHORT, LOCALE).replace(".", "")
                + " " + periodo.getYear();
    }
}

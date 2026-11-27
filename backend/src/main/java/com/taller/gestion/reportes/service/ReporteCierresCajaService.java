package com.taller.gestion.reportes.service;

import com.taller.gestion.caja.entity.EstadoSesionCaja;
import com.taller.gestion.caja.entity.MedioPago;
import com.taller.gestion.caja.entity.TipoMovimientoCaja;
import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.CierreCajaFilaDTO;
import com.taller.gestion.reportes.dto.ReporteCierresCajaDTO;
import com.taller.gestion.reportes.dto.TotalesCierresCajaDTO;
import com.taller.gestion.reportes.dto.ValoresMedioPagoCierreDTO;
import com.taller.gestion.reportes.repository.ReporteCierresCajaRepository;
import com.taller.gestion.reportes.repository.ResumenDetalleCierre;
import com.taller.gestion.reportes.repository.ResumenMovimientoCaja;
import com.taller.gestion.reportes.repository.ResumenSesionCaja;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Cierres de caja del rango: una fila por sesión y totales por medio de pago. */
@Service
@RequiredArgsConstructor
public class ReporteCierresCajaService {

    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final List<String> TODOS_LOS_MEDIOS =
            List.of(MedioPago.EFECTIVO.name(), MedioPago.TRANSFERENCIA.name());

    private final ReporteCierresCajaRepository reporteCierresCajaRepository;

    @Transactional(readOnly = true)
    public ReporteCierresCajaDTO cierresCaja(
            LocalDate desde, LocalDate hasta, Long idUsuarioCierre,
            String estadoTexto, String medioTexto) {

        validarRango(desde, hasta);
        EstadoSesionCaja estado = parsearEstado(estadoTexto);
        MedioPago medio = parsearMedio(medioTexto);
        List<String> medios = medio == null ? TODOS_LOS_MEDIOS : List.of(medio.name());

        // 'hasta' se incluye completo: el límite superior es el día siguiente.
        LocalDate hastaExclusivo = hasta.plusDays(1);
        List<ResumenSesionCaja> sesiones = reporteCierresCajaRepository.listarSesiones(
                desde, hastaExclusivo, idUsuarioCierre, estado);

        Map<Long, Map<String, ValoresMedioPagoCierreDTO>> porSesion = new HashMap<>();
        for (ResumenSesionCaja sesion : sesiones) {
            Map<String, ValoresMedioPagoCierreDTO> valores = new HashMap<>();
            for (String nombre : medios) {
                valores.put(nombre, ceros());
            }
            porSesion.put(sesion.idSesion(), valores);
        }

        for (ResumenDetalleCierre detalle : reporteCierresCajaRepository.listarDetalles(
                desde, hastaExclusivo, idUsuarioCierre, estado)) {
            String nombre = detalle.medioPago().name();
            Map<String, ValoresMedioPagoCierreDTO> valores = porSesion.get(detalle.idSesion());
            if (valores == null || !valores.containsKey(nombre)) {
                continue;
            }
            BigDecimal diferencia = normalizar(detalle.diferencia());
            valores.put(nombre, new ValoresMedioPagoCierreDTO(
                    normalizar(detalle.esperado()),
                    normalizar(detalle.declarado()),
                    diferencia,
                    valores.get(nombre).ingresos(),
                    valores.get(nombre).egresos(),
                    diferencia.signum() < 0 ? diferencia.abs() : CERO,
                    diferencia.signum() > 0 ? diferencia : CERO));
        }

        for (ResumenMovimientoCaja movimiento : reporteCierresCajaRepository.listarMovimientos(
                desde, hastaExclusivo, idUsuarioCierre, estado)) {
            String nombre = movimiento.medioPago().name();
            Map<String, ValoresMedioPagoCierreDTO> valores = porSesion.get(movimiento.idSesion());
            if (valores == null || !valores.containsKey(nombre)) {
                continue;
            }
            ValoresMedioPagoCierreDTO actual = valores.get(nombre);
            BigDecimal total = normalizar(movimiento.total());
            boolean ingreso = movimiento.tipo() == TipoMovimientoCaja.INGRESO;
            valores.put(nombre, new ValoresMedioPagoCierreDTO(
                    actual.esperado(), actual.declarado(), actual.diferencia(),
                    ingreso ? actual.ingresos().add(total) : actual.ingresos(),
                    ingreso ? actual.egresos() : actual.egresos().add(total),
                    actual.faltante(), actual.sobrante()));
        }

        List<CierreCajaFilaDTO> filas = new ArrayList<>();
        Map<String, ValoresMedioPagoCierreDTO> totalesPorMedio = new HashMap<>();
        for (String nombre : medios) {
            totalesPorMedio.put(nombre, ceros());
        }
        for (ResumenSesionCaja sesion : sesiones) {
            Map<String, ValoresMedioPagoCierreDTO> valores = porSesion.get(sesion.idSesion());
            filas.add(new CierreCajaFilaDTO(
                    sesion.idSesion(),
                    sesion.fechaApertura(), sesion.horaApertura(),
                    sesion.fechaCierre(), sesion.horaCierre(),
                    sesion.abrio(), sesion.cerro(),
                    normalizar(sesion.montoInicial()),
                    sesion.estado(),
                    sesion.estado() == EstadoSesionCaja.PENDIENTE_REVISION,
                    Map.copyOf(valores)));
            for (String nombre : medios) {
                totalesPorMedio.put(nombre,
                        sumar(totalesPorMedio.get(nombre), valores.get(nombre)));
            }
        }

        return new ReporteCierresCajaDTO(
                desde, hasta, idUsuarioCierre,
                estado == null ? "TODOS" : estado.name(),
                medio == null ? "TODOS" : medio.name(),
                medios, List.copyOf(filas),
                new TotalesCierresCajaDTO(filas.size(), Map.copyOf(totalesPorMedio)));
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new DatosInvalidosException("El rango de fechas es obligatorio");
        }
        if (desde.isAfter(hasta)) {
            throw new DatosInvalidosException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
    }

    /** ABIERTA queda fuera por decisión: no tiene cierre ni declarado. */
    private EstadoSesionCaja parsearEstado(String valor) {
        String texto = valor == null || valor.isBlank() ? "TODOS" : valor.trim().toUpperCase(Locale.ROOT);
        if ("TODOS".equals(texto)) {
            return null;
        }
        if ("ABIERTA".equals(texto)) {
            throw new DatosInvalidosException("El reporte no incluye sesiones abiertas");
        }
        try {
            return EstadoSesionCaja.valueOf(texto);
        } catch (IllegalArgumentException ex) {
            throw new DatosInvalidosException("El parámetro 'estado' tiene un valor inválido");
        }
    }

    private MedioPago parsearMedio(String valor) {
        String texto = valor == null || valor.isBlank() ? "TODOS" : valor.trim().toUpperCase(Locale.ROOT);
        if ("TODOS".equals(texto)) {
            return null;
        }
        try {
            return MedioPago.valueOf(texto);
        } catch (IllegalArgumentException ex) {
            throw new DatosInvalidosException("El parámetro 'medio_pago' tiene un valor inválido");
        }
    }

    private ValoresMedioPagoCierreDTO ceros() {
        return new ValoresMedioPagoCierreDTO(CERO, CERO, CERO, CERO, CERO, CERO, CERO);
    }

    private BigDecimal normalizar(BigDecimal valor) {
        return valor == null ? CERO : valor.setScale(2, RoundingMode.HALF_UP);
    }

    private ValoresMedioPagoCierreDTO sumar(ValoresMedioPagoCierreDTO a,
                                             ValoresMedioPagoCierreDTO b) {
        return new ValoresMedioPagoCierreDTO(
                a.esperado().add(b.esperado()),
                a.declarado().add(b.declarado()),
                a.diferencia().add(b.diferencia()),
                a.ingresos().add(b.ingresos()),
                a.egresos().add(b.egresos()),
                a.faltante().add(b.faltante()),
                a.sobrante().add(b.sobrante()));
    }
}

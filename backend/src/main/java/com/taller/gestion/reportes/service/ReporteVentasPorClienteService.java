package com.taller.gestion.reportes.service;

import com.taller.gestion.clientes.entity.TipoCliente;
import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.ReporteVentasPorClienteDTO;
import com.taller.gestion.reportes.dto.TotalesPorTipoClienteDTO;
import com.taller.gestion.reportes.dto.TotalesVentasPorClienteDTO;
import com.taller.gestion.reportes.dto.VentasPorClienteFilaDTO;
import com.taller.gestion.reportes.repository.ReporteVentasPorClienteRepository;
import com.taller.gestion.reportes.repository.ResumenVentasPorCliente;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReporteVentasPorClienteService {

    private static final String GRAN_CLIENTE = "GRAN_CLIENTE";
    private static final String CONSUMIDOR_FINAL = "CONSUMIDOR_FINAL";
    private static final String NOMBRE_GENERICO_MOSTRADOR = "Consumidor final (mostrador)";
    private static final BigDecimal CERO_TOTAL = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    /** El resumen siempre muestra los dos tipos, aunque no tengan ventas en el rango. */
    private static final List<String> TIPOS_RESUMEN = List.of(GRAN_CLIENTE, CONSUMIDOR_FINAL);

    private final ReporteVentasPorClienteRepository reporteVentasPorClienteRepository;

    @Transactional(readOnly = true)
    public ReporteVentasPorClienteDTO ventasPorCliente(
            LocalDate desde, LocalDate hasta, Long idCliente, String tipoCliente) {

        validarRango(desde, hasta);
        String tipoFiltrado = tipoCliente == null || tipoCliente.isBlank()
                ? null : tipoCliente.trim().toUpperCase(Locale.ROOT);
        List<TipoCliente> tipos = tiposDelFiltro(tipoFiltrado);

        // 'hasta' se incluye completo: el límite superior es el día siguiente.
        List<ResumenVentasPorCliente> consulta = reporteVentasPorClienteRepository.consultar(
                desde, hasta.plusDays(1), idCliente, tipos);

        List<VentasPorClienteFilaDTO> filas = consulta.stream()
                .map(this::aFila)
                .toList();
        long cantidadCompras = filas.stream()
                .mapToLong(VentasPorClienteFilaDTO::cantidadCompras)
                .sum();
        BigDecimal totalVendido = filas.stream()
                .map(VentasPorClienteFilaDTO::totalVendido)
                .reduce(CERO_TOTAL, BigDecimal::add);

        return new ReporteVentasPorClienteDTO(
                desde, hasta, idCliente, tipoFiltrado, filas, resumenPorTipo(filas),
                new TotalesVentasPorClienteDTO(
                        cantidadCompras,
                        totalVendido.setScale(2, RoundingMode.HALF_UP),
                        filas.size()));
    }

    /**
     * El filtro de negocio "Consumidor final" incluye al cliente genérico de mostrador, que en la
     * base tiene su propio tipo. Aclarado por Mayra el 27/11/2026.
     */
    private List<TipoCliente> tiposDelFiltro(String tipo) {
        if (tipo == null) {
            return null;
        }
        return switch (tipo) {
            case GRAN_CLIENTE -> List.of(TipoCliente.GRAN_CLIENTE);
            case CONSUMIDOR_FINAL ->
                    List.of(TipoCliente.CONSUMIDOR_FINAL, TipoCliente.GENERICO);
            default -> throw new DatosInvalidosException(
                    "El tipo de cliente debe ser GRAN_CLIENTE o CONSUMIDOR_FINAL");
        };
    }

    private VentasPorClienteFilaDTO aFila(ResumenVentasPorCliente resumen) {
        boolean generico = resumen.tipoCliente() == TipoCliente.GENERICO;
        String tipoUnificado = generico ? CONSUMIDOR_FINAL : resumen.tipoCliente().name();
        return new VentasPorClienteFilaDTO(
                resumen.idCliente(),
                generico ? NOMBRE_GENERICO_MOSTRADOR : resumen.nombre(),
                tipoUnificado,
                etiqueta(tipoUnificado),
                resumen.cantidadCompras(),
                resumen.totalVendido().setScale(2, RoundingMode.HALF_UP));
    }

    /** El resumen se calcula sobre las mismas filas de la tabla, así respeta todos los filtros. */
    private List<TotalesPorTipoClienteDTO> resumenPorTipo(List<VentasPorClienteFilaDTO> filas) {
        Map<String, Long> comprasPorTipo = new HashMap<>();
        Map<String, BigDecimal> totalPorTipo = new HashMap<>();
        for (VentasPorClienteFilaDTO fila : filas) {
            comprasPorTipo.merge(fila.tipo_cliente(), fila.cantidadCompras(), Long::sum);
            totalPorTipo.merge(fila.tipo_cliente(), fila.totalVendido(), BigDecimal::add);
        }
        return TIPOS_RESUMEN.stream()
                .map(tipo -> new TotalesPorTipoClienteDTO(
                        tipo,
                        etiqueta(tipo),
                        comprasPorTipo.getOrDefault(tipo, 0L),
                        totalPorTipo.getOrDefault(tipo, CERO_TOTAL)
                                .setScale(2, RoundingMode.HALF_UP)))
                .toList();
    }

    private String etiqueta(String tipo) {
        return GRAN_CLIENTE.equals(tipo) ? "Gran cliente" : "Consumidor final";
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new DatosInvalidosException("El rango de fechas es obligatorio");
        }
        if (desde.isAfter(hasta)) {
            throw new DatosInvalidosException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
    }
}

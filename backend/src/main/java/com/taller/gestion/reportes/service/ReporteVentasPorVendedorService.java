package com.taller.gestion.reportes.service;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.ReporteVentasPorVendedorDTO;
import com.taller.gestion.reportes.dto.TotalesVentasPorVendedorDTO;
import com.taller.gestion.reportes.dto.VentasPorVendedorFilaDTO;
import com.taller.gestion.reportes.repository.ReporteVentasPorVendedorRepository;
import com.taller.gestion.reportes.repository.ResumenVentasPorVendedor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReporteVentasPorVendedorService {

    private static final BigDecimal CERO_TOTAL = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final ReporteVentasPorVendedorRepository reporteVentasPorVendedorRepository;

    @Transactional(readOnly = true)
    public ReporteVentasPorVendedorDTO ventasPorVendedor(
            LocalDate desde, LocalDate hasta, Long idVendedor) {

        validarRango(desde, hasta);

        // 'hasta' se incluye completo: el límite superior es el día siguiente.
        List<ResumenVentasPorVendedor> consulta = reporteVentasPorVendedorRepository.consultar(
                desde, hasta.plusDays(1), idVendedor);

        // Los nombres repetidos se desambiguan con el email; se decide sobre las filas del reporte.
        Set<String> nombresRepetidos = nombresRepetidos(consulta);
        List<VentasPorVendedorFilaDTO> filas = consulta.stream()
                .map(resumen -> aFila(resumen, nombresRepetidos))
                .toList();

        long cantidadVentas = filas.stream()
                .mapToLong(VentasPorVendedorFilaDTO::cantidadVentas)
                .sum();
        BigDecimal totalVendido = filas.stream()
                .map(VentasPorVendedorFilaDTO::totalVendido)
                .reduce(CERO_TOTAL, BigDecimal::add);

        return new ReporteVentasPorVendedorDTO(
                desde, hasta, idVendedor, filas,
                new TotalesVentasPorVendedorDTO(
                        cantidadVentas,
                        totalVendido.setScale(2, RoundingMode.HALF_UP),
                        ticketPromedio(totalVendido, cantidadVentas),
                        filas.size()));
    }

    /** "Vendedor (ROL)", o "Vendedor (ROL, email)" cuando hay otro usuario con el mismo nombre. */
    private VentasPorVendedorFilaDTO aFila(
            ResumenVentasPorVendedor resumen, Set<String> nombresRepetidos) {

        BigDecimal total = resumen.totalVendido().setScale(2, RoundingMode.HALF_UP);
        String rol = resumen.rol().name();
        String etiqueta = nombresRepetidos.contains(nombreClave(resumen.nombre()))
                ? resumen.nombre() + " (" + rol + ", " + resumen.email() + ")"
                : resumen.nombre() + " (" + rol + ")";

        return new VentasPorVendedorFilaDTO(
                resumen.idUsuario(),
                etiqueta,
                resumen.cantidadVentas(),
                total,
                ticketPromedio(total, resumen.cantidadVentas()));
    }

    private Set<String> nombresRepetidos(List<ResumenVentasPorVendedor> consulta) {
        Map<String, Long> cantidades = new HashMap<>();
        for (ResumenVentasPorVendedor resumen : consulta) {
            cantidades.merge(nombreClave(resumen.nombre()), 1L, Long::sum);
        }
        Set<String> repetidos = new HashSet<>();
        cantidades.forEach((nombre, cantidad) -> {
            if (cantidad > 1) {
                repetidos.add(nombre);
            }
        });
        return repetidos;
    }

    private String nombreClave(String nombre) {
        return nombre == null ? "" : nombre.trim().toLowerCase(Locale.ROOT);
    }

    private BigDecimal ticketPromedio(BigDecimal total, long cantidad) {
        if (cantidad == 0) {
            return CERO_TOTAL;
        }
        return total.divide(BigDecimal.valueOf(cantidad), 2, RoundingMode.HALF_UP);
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

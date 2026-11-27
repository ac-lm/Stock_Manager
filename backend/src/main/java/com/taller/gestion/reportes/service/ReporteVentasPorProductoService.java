package com.taller.gestion.reportes.service;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.ReporteVentasPorProductoDTO;
import com.taller.gestion.reportes.dto.TotalesVentasPorProductoDTO;
import com.taller.gestion.reportes.dto.VentasPorProductoFilaDTO;
import com.taller.gestion.reportes.repository.ReporteVentasPorProductoRepository;
import com.taller.gestion.reportes.repository.ResumenVentasPorProducto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReporteVentasPorProductoService {

    public static final int TOP_DEFECTO = 10;

    private static final BigDecimal CERO_CANTIDAD = BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
    private static final BigDecimal CERO_TOTAL = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final Comparator<VentasPorProductoFilaDTO> POR_TOTAL =
            Comparator.comparing(VentasPorProductoFilaDTO::totalVendido)
                    .reversed()
                    .thenComparing(VentasPorProductoFilaDTO::producto);

    private final ReporteVentasPorProductoRepository reporteVentasPorProductoRepository;

    @Transactional(readOnly = true)
    public ReporteVentasPorProductoDTO ventasPorProducto(
            LocalDate desde, LocalDate hasta, Long idProducto, String categoria, Integer top) {

        validarRango(desde, hasta);
        int limite = validarTop(top);
        String categoriaFiltrada = categoria == null || categoria.isBlank() ? null : categoria.trim();

        // 'hasta' se incluye completo: el límite superior es el día siguiente.
        List<ResumenVentasPorProducto> consulta = reporteVentasPorProductoRepository.consultar(
                desde, hasta.plusDays(1), idProducto, categoriaFiltrada);

        List<VentasPorProductoFilaDTO> filas = consulta.stream()
                .map(this::aFila)
                .toList();

        BigDecimal cantidadVendida = filas.stream()
                .map(VentasPorProductoFilaDTO::cantidadVendida)
                .reduce(CERO_CANTIDAD, BigDecimal::add);
        BigDecimal totalVendido = filas.stream()
                .map(VentasPorProductoFilaDTO::totalVendido)
                .reduce(CERO_TOTAL, BigDecimal::add);

        return new ReporteVentasPorProductoDTO(
                desde, hasta, idProducto, categoriaFiltrada, limite, filas,
                filas.stream().sorted(POR_TOTAL).limit(limite).toList(),
                new TotalesVentasPorProductoDTO(
                        cantidadVendida.setScale(3, RoundingMode.HALF_UP),
                        totalVendido.setScale(2, RoundingMode.HALF_UP),
                        filas.size()));
    }

    private VentasPorProductoFilaDTO aFila(ResumenVentasPorProducto resumen) {
        return new VentasPorProductoFilaDTO(
                resumen.idProducto(),
                resumen.nombre(),
                resumen.categoria(),
                resumen.cantidadVendida().setScale(3, RoundingMode.HALF_UP),
                resumen.totalVendido().setScale(2, RoundingMode.HALF_UP));
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new DatosInvalidosException("El rango de fechas es obligatorio");
        }
        if (desde.isAfter(hasta)) {
            throw new DatosInvalidosException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
    }

    private int validarTop(Integer top) {
        if (top == null) {
            return TOP_DEFECTO;
        }
        if (top < 1) {
            throw new DatosInvalidosException("El parámetro 'top' debe ser un entero positivo");
        }
        return top;
    }
}

package com.taller.gestion.reportes.service;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.MovimientoStockFilaDTO;
import com.taller.gestion.reportes.dto.ReporteMovimientosStockDTO;
import com.taller.gestion.reportes.dto.TipoArticuloStock;
import com.taller.gestion.reportes.dto.TotalesMovimientosStockDTO;
import com.taller.gestion.reportes.repository.ReporteMovimientosStockRepository;
import com.taller.gestion.reportes.repository.ResumenMovimientoStock;
import com.taller.gestion.stock.entity.TipoMovimientoStock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * Movimientos agrupados por artículo. Entradas = todas las ENTRADA; una salida es por venta cuando
 * lleva id_venta, descarte si es MERMA, ajuste si es AJUSTE y "otras salidas" en cualquier otro caso.
 */
@Service
@RequiredArgsConstructor
public class ReporteMovimientosStockService {

    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);

    private final ReporteMovimientosStockRepository reporteMovimientosStockRepository;

    @Transactional(readOnly = true)
    public ReporteMovimientosStockDTO movimientosStock(
            LocalDate desde, LocalDate hasta, String tipoArticuloTexto, Long idArticulo,
            String tipoMovimientoTexto, Long idProveedor) {

        validarRango(desde, hasta);
        TipoArticuloStock tipoArticulo = parsearTipoArticulo(tipoArticuloTexto);
        TipoMovimientoStock tipoMovimiento = parsearTipoMovimiento(tipoMovimientoTexto);
        if (idArticulo != null && tipoArticulo == null) {
            throw new DatosInvalidosException(
                    "El artículo requiere indicar si es producto o insumo");
        }

        // 'hasta' se incluye completo: el límite superior es el día siguiente.
        List<ResumenMovimientoStock> resumenes = reporteMovimientosStockRepository.consultar(
                desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay(),
                tipoArticulo, idArticulo, tipoMovimiento, idProveedor);
        List<MovimientoStockFilaDTO> filas = resumenes.stream().map(this::aFila).toList();

        return new ReporteMovimientosStockDTO(
                desde, hasta,
                tipoArticulo == null ? "TODOS" : tipoArticulo.name(), idArticulo,
                tipoMovimiento == null ? "TODOS" : tipoMovimiento.name(), idProveedor,
                filas,
                new TotalesMovimientosStockDTO(
                        filas.size(), resumenes.stream().mapToLong(ResumenMovimientoStock::movimientos).sum()));
    }

    private MovimientoStockFilaDTO aFila(ResumenMovimientoStock resumen) {
        return new MovimientoStockFilaDTO(
                resumen.idArticulo(),
                TipoArticuloStock.valueOf(resumen.tipo()),
                resumen.nombre(),
                resumen.unidad(),
                normalizar(resumen.entradas()),
                normalizar(resumen.salidasVenta()),
                normalizar(resumen.descartes()),
                normalizar(resumen.ajustes()),
                normalizar(resumen.otrasSalidas()));
    }

    private BigDecimal normalizar(BigDecimal valor) {
        return valor == null ? CERO : valor.setScale(3, RoundingMode.HALF_UP);
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new DatosInvalidosException("El rango de fechas es obligatorio");
        }
        if (desde.isAfter(hasta)) {
            throw new DatosInvalidosException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
    }

    private TipoArticuloStock parsearTipoArticulo(String valor) {
        String texto = valor == null || valor.isBlank() ? "TODOS" : valor.trim().toUpperCase(Locale.ROOT);
        if ("TODOS".equals(texto)) {
            return null;
        }
        try {
            return TipoArticuloStock.valueOf(texto);
        } catch (IllegalArgumentException ex) {
            throw new DatosInvalidosException(
                    "El parámetro 'tipo_articulo' tiene un valor inválido");
        }
    }

    private TipoMovimientoStock parsearTipoMovimiento(String valor) {
        String texto = valor == null || valor.isBlank() ? "TODOS" : valor.trim().toUpperCase(Locale.ROOT);
        if ("TODOS".equals(texto)) {
            return null;
        }
        try {
            return TipoMovimientoStock.valueOf(texto);
        } catch (IllegalArgumentException ex) {
            throw new DatosInvalidosException(
                    "El parámetro 'tipo_movimiento' tiene un valor inválido");
        }
    }
}

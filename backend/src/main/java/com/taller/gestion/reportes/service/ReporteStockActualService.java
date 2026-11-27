package com.taller.gestion.reportes.service;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.ReporteStockActualDTO;
import com.taller.gestion.reportes.dto.StockActualFilaDTO;
import com.taller.gestion.reportes.dto.TipoArticuloStock;
import com.taller.gestion.reportes.dto.TotalesStockActualDTO;
import com.taller.gestion.reportes.exportacion.ReportesExportacionConfig;
import com.taller.gestion.reportes.repository.ReporteStockActualRepository;
import com.taller.gestion.reportes.repository.ResumenStockActual;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Foto de hoy del stock; el mínimo existe solo en insumos y se compara con < estricto. */
@Service
@RequiredArgsConstructor
public class ReporteStockActualService {

    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
    private static final Comparator<StockActualFilaDTO> POR_FALTA =
            Comparator.comparing(StockActualFilaDTO::falta)
                    .reversed()
                    .thenComparing(StockActualFilaDTO::articulo);

    private final ReporteStockActualRepository reporteStockActualRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ReporteStockActualDTO stockActual(
            String tipoTexto, String categoriaTexto, boolean soloBajoMinimo) {

        TipoArticuloStock tipo = parsearTipo(tipoTexto);
        String categoria = categoriaTexto == null || categoriaTexto.isBlank()
                ? null : categoriaTexto.trim();
        LocalDate hoy = LocalDate.now(clock.withZone(ReportesExportacionConfig.ZONA_TALLER));

        List<StockActualFilaDTO> consultadas = reporteStockActualRepository.consultar(tipo, categoria)
                .stream()
                .map(this::aFila)
                .toList();
        List<StockActualFilaDTO> bajoMinimo = consultadas.stream()
                .filter(StockActualFilaDTO::bajoMinimo)
                .sorted(POR_FALTA)
                .toList();
        List<StockActualFilaDTO> filas = soloBajoMinimo ? bajoMinimo : consultadas;

        return new ReporteStockActualDTO(
                hoy, hoy, tipo == null ? "TODOS" : tipo.name(), categoria, soloBajoMinimo,
                filas, bajoMinimo,
                new TotalesStockActualDTO(filas.size(), bajoMinimo.size()));
    }

    private StockActualFilaDTO aFila(ResumenStockActual resumen) {
        BigDecimal stock = normalizar(resumen.stockActual());
        if (TipoArticuloStock.PRODUCTO.name().equals(resumen.tipo())) {
            // Producto no tiene stock_minimo ni unidad: N/A y nunca se marca como bajo mínimo.
            return new StockActualFilaDTO(
                    resumen.idArticulo(), TipoArticuloStock.PRODUCTO, resumen.nombre(),
                    resumen.categoria(), null, stock, null, null, false);
        }

        // Insumo sin mínimo cargado: 0, igual que hace StockService.
        int minimo = resumen.stockMinimo() == null ? 0 : resumen.stockMinimo();
        BigDecimal minimoDecimal = BigDecimal.valueOf(minimo);
        BigDecimal falta = minimoDecimal.subtract(stock)
                .max(BigDecimal.ZERO)
                .setScale(3, RoundingMode.HALF_UP);
        return new StockActualFilaDTO(
                resumen.idArticulo(), TipoArticuloStock.INSUMO, resumen.nombre(),
                null, resumen.unidad(), stock, minimo, falta,
                stock.compareTo(minimoDecimal) < 0);
    }

    private BigDecimal normalizar(BigDecimal valor) {
        return valor == null ? CERO : valor.setScale(3, RoundingMode.HALF_UP);
    }

    private TipoArticuloStock parsearTipo(String valor) {
        String texto = valor == null || valor.isBlank() ? "TODOS" : valor.trim().toUpperCase(Locale.ROOT);
        if ("TODOS".equals(texto)) {
            return null;
        }
        try {
            return TipoArticuloStock.valueOf(texto);
        } catch (IllegalArgumentException ex) {
            throw new DatosInvalidosException("El parámetro 'tipo' tiene un valor inválido");
        }
    }
}

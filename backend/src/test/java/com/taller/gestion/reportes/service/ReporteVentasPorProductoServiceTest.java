package com.taller.gestion.reportes.service;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.ReporteVentasPorProductoDTO;
import com.taller.gestion.reportes.repository.ReporteVentasPorProductoRepository;
import com.taller.gestion.reportes.repository.ResumenVentasPorProducto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteVentasPorProductoServiceTest {

    @Mock
    private ReporteVentasPorProductoRepository reporteVentasPorProductoRepository;

    @InjectMocks
    private ReporteVentasPorProductoService service;

    private List<ResumenVentasPorProducto> consulta() {
        return List.of(
                new ResumenVentasPorProducto(1L, "Café", "Bebidas",
                        new BigDecimal("3.000"), new BigDecimal("150.00")),
                new ResumenVentasPorProducto(2L, "Torta", "Repostería",
                        new BigDecimal("2.000"), new BigDecimal("300.00")),
                new ResumenVentasPorProducto(3L, "Mate", "Bebidas",
                        new BigDecimal("1.000"), new BigDecimal("320.00")));
    }

    @Test
    @DisplayName("Faltan fechas: 400 de negocio, sin consultar")
    void fechasAusentes_lanzanDatosInvalidos() {
        assertThatThrownBy(() -> service.ventasPorProducto(null, null, null, null, 10))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El rango de fechas es obligatorio");
        verify(reporteVentasPorProductoRepository, never()).consultar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Rango invertido: 400 de negocio, sin consultar")
    void rangoInvertido_lanzaDatosInvalidos() {
        assertThatThrownBy(() -> service.ventasPorProducto(
                LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 1), null, null, 10))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("La fecha 'desde' no puede ser posterior a 'hasta'");
        verify(reporteVentasPorProductoRepository, never()).consultar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Top cero: 400 de negocio, sin consultar")
    void topCero_lanzaDatosInvalidos() {
        assertThatThrownBy(() -> service.ventasPorProducto(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), null, null, 0))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El parámetro 'top' debe ser un entero positivo");
        verify(reporteVentasPorProductoRepository, never()).consultar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("El día 'hasta' se incluye y los filtros llegan al repositorio")
    void hastaInclusivo_yFiltros_pasanAlRepositorio() {
        when(reporteVentasPorProductoRepository.consultar(any(), any(), any(), any()))
                .thenReturn(consulta());

        service.ventasPorProducto(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                7L, " Bebidas ", 2);

        verify(reporteVentasPorProductoRepository).consultar(
                eq(LocalDate.of(2026, 10, 1)), eq(LocalDate.of(2026, 11, 1)),
                eq(7L), eq("Bebidas"));
    }

    @Test
    @DisplayName("Totales, filas y exportación: cantidades con 3 decimales y montos con 2")
    void totalesFilasYExportacion_tienenLaEscalaAcordada() {
        when(reporteVentasPorProductoRepository.consultar(any(), any(), any(), any()))
                .thenReturn(consulta());

        ReporteVentasPorProductoDTO reporte = service.ventasPorProducto(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), null, null, null);

        assertThat(reporte.filas()).hasSize(3);
        assertThat(reporte.filas().get(0).cantidadVendida()).isEqualByComparingTo("3.000");
        assertThat(reporte.filas().get(0).cantidadVendida().scale()).isEqualTo(3);
        assertThat(reporte.filas().get(0).totalVendido().scale()).isEqualTo(2);
        assertThat(reporte.totales().cantidadVendida()).isEqualByComparingTo("6.000");
        assertThat(reporte.totales().totalVendido()).isEqualByComparingTo("770.00");
        assertThat(reporte.totales().productos()).isEqualTo(3);
        assertThat(reporte.columnas()).extracting("titulo")
                .containsExactly("Producto", "Categoría", "Cantidad vendida", "Total vendido");
        assertThat(reporte.filasExportables().get(0)).containsExactly(
                "Café", "Bebidas", new BigDecimal("3.000"), new BigDecimal("150.00"));
        assertThat(reporte.totalesExportables()).containsExactly(
                "Totales", "", new BigDecimal("6.000"), new BigDecimal("770.00"));
        assertThat(reporte.descripcionFiltros()).isEqualTo("Top 10 por total vendido");
    }

    @Test
    @DisplayName("Ranking: los N de mayor total, con desempate por nombre")
    void ranking_ordenaPorTotalYRespetaTop() {
        when(reporteVentasPorProductoRepository.consultar(any(), any(), any(), any()))
                .thenReturn(consulta());

        ReporteVentasPorProductoDTO reporte = service.ventasPorProducto(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), null, null, 2);

        assertThat(reporte.top()).isEqualTo(2);
        assertThat(reporte.ranking()).hasSize(2);
        assertThat(reporte.ranking()).extracting("producto").containsExactly("Mate", "Torta");
        assertThat(reporte.filas()).hasSize(3); // el tope no achica la tabla
        assertThat(reporte.descripcionFiltros()).isEqualTo("Top 2 por total vendido");
    }

    @Test
    @DisplayName("Sin productos: filas, ranking y totales en cero")
    void sinVentas_devuelveListasVaciasYTotalesEnCero() {
        when(reporteVentasPorProductoRepository.consultar(any(), any(), any(), any()))
                .thenReturn(List.of());

        ReporteVentasPorProductoDTO reporte = service.ventasPorProducto(
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 3), null, null, 10);

        assertThat(reporte.filas()).isEmpty();
        assertThat(reporte.ranking()).isEmpty();
        assertThat(reporte.totales().cantidadVendida()).isEqualByComparingTo("0.000");
        assertThat(reporte.totales().totalVendido()).isEqualByComparingTo("0.00");
        assertThat(reporte.totales().productos()).isZero();
        assertThat(reporte.totalesExportables()).containsExactly(
                "Totales", "", new BigDecimal("0.000"), new BigDecimal("0.00"));
    }
}

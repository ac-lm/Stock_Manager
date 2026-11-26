package com.taller.gestion.reportes.service;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.AgrupacionPeriodo;
import com.taller.gestion.reportes.dto.ReporteVentasPorPeriodoDTO;
import com.taller.gestion.reportes.repository.ReporteVentasRepository;
import com.taller.gestion.reportes.repository.ResumenVentasPorPeriodo;
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
class ReporteVentasServiceTest {

    @Mock
    private ReporteVentasRepository reporteVentasRepository;

    @InjectMocks
    private ReporteVentasService reporteVentasService;

    @Test
    @DisplayName("Rango invertido: 400 de negocio, sin consultar")
    void rangoInvertido_lanzaDatosInvalidos() {
        assertThatThrownBy(() -> reporteVentasService.ventasPorPeriodo(
                LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 1), "dia"))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("La fecha 'desde' no puede ser posterior a 'hasta'");
        verify(reporteVentasRepository, never()).consultar(any(), any(), any());
    }

    @Test
    @DisplayName("Faltan fechas: 400 de negocio, sin consultar")
    void fechasAusentes_lanzanDatosInvalidos() {
        assertThatThrownBy(() -> reporteVentasService.ventasPorPeriodo(null, null, "dia"))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El rango de fechas es obligatorio");
        verify(reporteVentasRepository, never()).consultar(any(), any(), any());
    }

    @Test
    @DisplayName("Agrupación desconocida: 400 de negocio, sin consultar")
    void agrupacionInvalida_lanzaDatosInvalidos() {
        assertThatThrownBy(() -> reporteVentasService.ventasPorPeriodo(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2), "anio"))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("La agrupación debe ser dia, semana o mes");
        verify(reporteVentasRepository, never()).consultar(any(), any(), any());
    }

    @Test
    @DisplayName("Más de 5 años con agrupación diaria: 400 de negocio, sin consultar")
    void rangoDiarioMayorACincoAnios_lanzaDatosInvalidos() {
        assertThatThrownBy(() -> reporteVentasService.ventasPorPeriodo(
                LocalDate.of(2021, 1, 1), LocalDate.of(2026, 1, 2), "dia"))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("5 años");
        verify(reporteVentasRepository, never()).consultar(any(), any(), any());
    }

    @Test
    @DisplayName("5 años justos con agrupación diaria: se permite")
    void rangoDiarioDeCincoAnios_sePermite() {
        when(reporteVentasRepository.consultar(any(), any(), any())).thenReturn(List.of());

        ReporteVentasPorPeriodoDTO reporte = reporteVentasService.ventasPorPeriodo(
                LocalDate.of(2021, 1, 1), LocalDate.of(2026, 1, 1), "dia");

        assertThat(reporte.filas()).isNotEmpty();
        verify(reporteVentasRepository).consultar(
                eq(AgrupacionPeriodo.DIA), eq(LocalDate.of(2021, 1, 1)), eq(LocalDate.of(2026, 1, 2)));
    }

    @Test
    @DisplayName("Más de 5 años con agrupación mensual: se permite")
    void rangoMensualLargo_sePermite() {
        when(reporteVentasRepository.consultar(any(), any(), any())).thenReturn(List.of());

        reporteVentasService.ventasPorPeriodo(
                LocalDate.of(2010, 1, 1), LocalDate.of(2026, 10, 1), "mes");

        verify(reporteVentasRepository).consultar(
                eq(AgrupacionPeriodo.MES), eq(LocalDate.of(2010, 1, 1)), eq(LocalDate.of(2026, 10, 2)));
    }

    @Test
    @DisplayName("El día 'hasta' se incluye: se consulta con hasta + 1 día")
    void hastaInclusivo_pasaElDiaSiguienteAlRepositorio() {
        when(reporteVentasRepository.consultar(any(), any(), any())).thenReturn(List.of());

        reporteVentasService.ventasPorPeriodo(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), "dia");

        verify(reporteVentasRepository).consultar(
                eq(AgrupacionPeriodo.DIA), eq(LocalDate.of(2026, 10, 1)), eq(LocalDate.of(2026, 11, 1)));
    }

    @Test
    @DisplayName("→ 'semana' y 'mes' en mayúsculas también se aceptan")
    void agrupacionEnMayusculas_seAcepta() {
        when(reporteVentasRepository.consultar(any(), any(), any())).thenReturn(List.of(
                new ResumenVentasPorPeriodo(LocalDate.of(2026, 9, 28),
                        new BigDecimal("100.00"), 1)));

        ReporteVentasPorPeriodoDTO reporte = reporteVentasService.ventasPorPeriodo(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 1), " SEMANA ");

        assertThat(reporte.agrupacion()).isEqualTo(AgrupacionPeriodo.SEMANA);
        assertThat(reporte.filas()).hasSize(1);
        assertThat(reporte.filas().get(0).periodo()).isEqualTo("2026-09-28");
        assertThat(reporte.filas().get(0).ticketPromedio()).isEqualByComparingTo("100.00");
        assertThat(reporte.totales().cantidadVentas()).isEqualTo(1);
        verify(reporteVentasRepository).consultar(any(), any(), any());
    }
}

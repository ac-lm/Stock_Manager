package com.taller.gestion.reportes.exportacion;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.ReporteResultadoPeriodoDTO;
import com.taller.gestion.reportes.service.ReporteCostosPorTipoService;
import com.taller.gestion.reportes.service.ReporteResultadoPeriodoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProveedorReporteResultadoPeriodoTest {

    @Mock
    private ReporteResultadoPeriodoService service;

    @InjectMocks
    private ProveedorReporteResultadoPeriodo proveedor;

    private ReporteResultadoPeriodoDTO respuesta() {
        return new ReporteResultadoPeriodoDTO(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 30),
                "2026-10", "2026-11", List.of(),
                new com.taller.gestion.reportes.dto.TotalesResultadoPeriodoDTO(
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 2),
                ReporteResultadoPeriodoService.ACLARACION,
                ReporteCostosPorTipoService.AVISO_INSUMOS);
    }

    @Test
    @DisplayName("Registrado con el id que usa la pantalla")
    void identificadoConElIdDelReporte() {
        assertThat(proveedor.id()).isEqualTo("resultado-periodo");
    }

    @Test
    @DisplayName("Convierte los meses de la URL en los parámetros del service")
    void filtrosConParametros_llamaAlService() {
        ReporteResultadoPeriodoDTO esperado = respuesta();
        when(service.resultadoPeriodo(any(), any())).thenReturn(esperado);

        ReporteExportable obtenido = proveedor.obtener(Map.of(
                "desde", "2026-10", "hasta", "2026-11"));

        assertThat(obtenido).isSameAs(esperado);
        verify(service).resultadoPeriodo(YearMonth.of(2026, 10), YearMonth.of(2026, 11));
    }

    @Test
    @DisplayName("El nombre del archivo lleva meses, no fechas completas")
    void nombreRangoSonMeses() {
        assertThat(respuesta().nombreRango()).isEqualTo("2026-10_2026-11");
    }

    @Test
    @DisplayName("El encabezado lleva los dos avisos y explica de dónde sale cada columna")
    void descripcionFiltrosIncluyeAvisos() {
        assertThat(respuesta().descripcionFiltros())
                .contains("fecha de venta, solo confirmadas")
                .contains("periodo_anio/periodo_mes, anulados excluidos")
                .contains("no un balance contable")
                .contains("CST-06");
    }

    @Test
    @DisplayName("Rango ausente o inválido: 400 y no llama al service")
    void parametrosInvalidos_lanzaDatosInvalidos() {
        assertThatThrownBy(() -> proveedor.obtener(Map.of("hasta", "2026-11")))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El parámetro 'desde' es obligatorio");
        assertThatThrownBy(() -> proveedor.obtener(Map.of(
                "desde", "2026-1", "hasta", "2026-11")))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El parámetro 'desde' tiene un formato inválido");
        verify(service, never()).resultadoPeriodo(any(), any());
    }
}

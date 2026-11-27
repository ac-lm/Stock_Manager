package com.taller.gestion.reportes.exportacion;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.ReporteCostosPorTipoDTO;
import com.taller.gestion.reportes.service.ReporteCostosPorTipoService;
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
class ProveedorReporteCostosPorTipoTest {

    @Mock
    private ReporteCostosPorTipoService service;

    @InjectMocks
    private ProveedorReporteCostosPorTipo proveedor;

    private ReporteCostosPorTipoDTO respuesta() {
        return new ReporteCostosPorTipoDTO(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 30),
                "2026-10", "2026-11", null,
                List.of(), List.of(), List.of(),
                new com.taller.gestion.reportes.dto.TotalesCostosPorTipoDTO(
                        BigDecimal.ZERO, 2, 0, 0),
                ReporteCostosPorTipoService.AVISO_INSUMOS);
    }

    @Test
    @DisplayName("Registrado con el id que usa la pantalla")
    void identificadoConElIdDelReporte() {
        assertThat(proveedor.id()).isEqualTo("costos-por-tipo");
    }

    @Test
    @DisplayName("Convierte los períodos y el tipo de la URL en los parámetros del service")
    void filtrosConParametros_llamaAlService() {
        ReporteCostosPorTipoDTO esperado = respuesta();
        when(service.costosPorTipo(any(), any(), any())).thenReturn(esperado);

        ReporteExportable obtenido = proveedor.obtener(Map.of(
                "desde", "2026-10", "hasta", "2026-11", "id_tipo_costo", "3"));

        assertThat(obtenido).isSameAs(esperado);
        verify(service).costosPorTipo(YearMonth.of(2026, 10), YearMonth.of(2026, 11), 3L);
    }

    @Test
    @DisplayName("Tipo ausente: todos los tipos")
    void filtroOpcionalAusente_usaDefecto() {
        when(service.costosPorTipo(any(), any(), any())).thenReturn(respuesta());

        proveedor.obtener(Map.of("desde", "2026-10", "hasta", "2026-11"));

        verify(service).costosPorTipo(YearMonth.of(2026, 10), YearMonth.of(2026, 11), null);
    }

    @Test
    @DisplayName("El nombre del archivo lleva meses, no fechas completas")
    void nombreRangoSonMeses() {
        assertThat(respuesta().nombreRango()).isEqualTo("2026-10_2026-11");
    }

    @Test
    @DisplayName("Rango ausente o inválido: 400 y no llama al service")
    void parametrosInvalidos_lanzaDatosInvalidos() {
        assertThatThrownBy(() -> proveedor.obtener(Map.of("hasta", "2026-11")))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> proveedor.obtener(Map.of(
                "desde", "2026-1", "hasta", "2026-11")))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El parámetro 'desde' tiene un formato inválido");
        verify(service, never()).costosPorTipo(any(), any(), any());
    }
}

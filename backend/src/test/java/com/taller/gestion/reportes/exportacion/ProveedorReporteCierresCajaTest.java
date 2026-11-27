package com.taller.gestion.reportes.exportacion;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.ReporteCierresCajaDTO;
import com.taller.gestion.reportes.service.ReporteCierresCajaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProveedorReporteCierresCajaTest {

    @Mock
    private ReporteCierresCajaService service;

    @InjectMocks
    private ProveedorReporteCierresCaja proveedor;

    @Test
    @DisplayName("Registrado con el id que usa la pantalla")
    void identificadoConElIdDelReporte() {
        assertThat(proveedor.id()).isEqualTo("cierres-caja");
    }

    @Test
    @DisplayName("Convierte los filtros de la URL en los parámetros del service")
    void filtrosConParametros_llamaAlService() {
        ReporteCierresCajaDTO esperado = new ReporteCierresCajaDTO(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), 7L,
                "PENDIENTE_REVISION", "EFECTIVO", List.of("EFECTIVO"), List.of(), null);
        when(service.cierresCaja(any(), any(), any(), any(), any())).thenReturn(esperado);

        ReporteExportable obtenido = proveedor.obtener(Map.of(
                "desde", "2026-10-01", "hasta", "2026-10-31",
                "id_usuario_cierre", "7", "estado", "PENDIENTE_REVISION",
                "medio_pago", "EFECTIVO"));

        assertThat(obtenido).isSameAs(esperado);
        verify(service).cierresCaja(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                7L, "PENDIENTE_REVISION", "EFECTIVO");
    }

    @Test
    @DisplayName("Filtros vacíos: sin usuario, estado y medio por defecto")
    void filtrosVacios_usaDefectos() {
        when(service.cierresCaja(any(), any(), any(), any(), any())).thenReturn(null);

        proveedor.obtener(Map.of("desde", "2026-10-01", "hasta", "2026-10-31",
                "id_usuario_cierre", "", "estado", "", "medio_pago", ""));

        verify(service).cierresCaja(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                null, "TODOS", "TODOS");
    }

    @Test
    @DisplayName("Fecha obligatoria ausente: 400 y no llama al service")
    void sinFecha_lanzaDatosInvalidos() {
        assertThatThrownBy(() -> proveedor.obtener(Map.of("hasta", "2026-10-31")))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El parámetro 'desde' es obligatorio");
        verify(service, never()).cierresCaja(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Usuario con formato inválido: 400 y no llama al service")
    void usuarioInvalido_lanzaDatosInvalidos() {
        assertThatThrownBy(() -> proveedor.obtener(Map.of(
                "desde", "2026-10-01", "hasta", "2026-10-31", "id_usuario_cierre", "abc")))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El parámetro 'id_usuario_cierre' tiene un formato inválido");
        verify(service, never()).cierresCaja(any(), any(), any(), any(), any());
    }
}

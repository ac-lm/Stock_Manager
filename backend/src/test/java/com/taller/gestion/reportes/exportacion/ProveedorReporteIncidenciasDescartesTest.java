package com.taller.gestion.reportes.exportacion;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.ReporteIncidenciasDescartesDTO;
import com.taller.gestion.reportes.dto.TotalesIncidenciasDescartesDTO;
import com.taller.gestion.reportes.service.ReporteIncidenciasDescartesService;
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
class ProveedorReporteIncidenciasDescartesTest {

    @Mock
    private ReporteIncidenciasDescartesService service;

    @InjectMocks
    private ProveedorReporteIncidenciasDescartes proveedor;

    private ReporteIncidenciasDescartesDTO respuesta() {
        return new ReporteIncidenciasDescartesDTO(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                null, "TODOS", "TODOS", null, List.of(), List.of(),
                new TotalesIncidenciasDescartesDTO(0, 0, 0, 0, 0), List.of(), List.of());
    }

    @Test
    @DisplayName("Registrado con el id del archivo de REP-13")
    void identificadoConElIdDelReporte() {
        assertThat(proveedor.id()).isEqualTo("incidencias-descartes");
    }

    @Test
    @DisplayName("Convierte los filtros de la URL en los parámetros del service")
    void filtrosConParametros_llamaAlService() {
        ReporteIncidenciasDescartesDTO esperado = respuesta();
        when(service.incidenciasDescartes(any(), any(), any(), any(), any(), any()))
                .thenReturn(esperado);

        ReporteExportable obtenido = proveedor.obtener(Map.of(
                "desde", "2026-11-01", "hasta", "2026-11-30",
                "id_tipo_incidencia", "101", "estado", "ABIERTA",
                "tipo_articulo", "INSUMO", "id_articulo", "10"));

        assertThat(obtenido).isSameAs(esperado);
        verify(service).incidenciasDescartes(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                101L, "ABIERTA", "INSUMO", 10L);
    }

    @Test
    @DisplayName("Filtros opcionales ausentes: todos los tipos, estados y artículos")
    void filtrosOpcionalesAusentes_usaDefectos() {
        when(service.incidenciasDescartes(any(), any(), any(), any(), any(), any()))
                .thenReturn(respuesta());

        proveedor.obtener(Map.of("desde", "2026-11-01", "hasta", "2026-11-30"));

        verify(service).incidenciasDescartes(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                null, null, null, null);
    }

    @Test
    @DisplayName("Rango ausente, fecha inválida o id inválido: 400 y no llama al service")
    void parametrosInvalidos_lanzaDatosInvalidos() {
        assertThatThrownBy(() -> proveedor.obtener(Map.of("hasta", "2026-11-30")))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> proveedor.obtener(Map.of(
                "desde", "ayer", "hasta", "2026-11-30")))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El parámetro 'desde' debe tener el formato AAAA-MM-DD");
        assertThatThrownBy(() -> proveedor.obtener(Map.of(
                "desde", "2026-11-01", "hasta", "2026-11-30", "id_articulo", "abc")))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El parámetro 'id_articulo' debe ser un número");
        verify(service, never()).incidenciasDescartes(any(), any(), any(), any(), any(), any());
    }
}

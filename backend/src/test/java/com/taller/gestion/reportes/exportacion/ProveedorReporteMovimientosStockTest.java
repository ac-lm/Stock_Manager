package com.taller.gestion.reportes.exportacion;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.ReporteMovimientosStockDTO;
import com.taller.gestion.reportes.dto.TotalesMovimientosStockDTO;
import com.taller.gestion.reportes.service.ReporteMovimientosStockService;
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
class ProveedorReporteMovimientosStockTest {

    @Mock
    private ReporteMovimientosStockService service;

    @InjectMocks
    private ProveedorReporteMovimientosStock proveedor;

    private ReporteMovimientosStockDTO respuesta() {
        return new ReporteMovimientosStockDTO(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                "TODOS", null, "TODOS", null, List.of(),
                new TotalesMovimientosStockDTO(0, 0));
    }

    @Test
    @DisplayName("Registrado con el id que usa la pantalla")
    void identificadoConElIdDelReporte() {
        assertThat(proveedor.id()).isEqualTo("movimientos-stock");
    }

    @Test
    @DisplayName("Convierte los filtros de la URL en los parámetros del service")
    void filtrosConParametros_llamaAlService() {
        ReporteMovimientosStockDTO esperado = respuesta();
        when(service.movimientosStock(any(), any(), any(), any(), any(), any()))
                .thenReturn(esperado);

        ReporteExportable obtenido = proveedor.obtener(Map.of(
                "desde", "2026-11-01", "hasta", "2026-11-30",
                "tipo_articulo", "INSUMO", "id_articulo", "10",
                "tipo_movimiento", "SALIDA", "id_proveedor", "3"));

        assertThat(obtenido).isSameAs(esperado);
        verify(service).movimientosStock(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                "INSUMO", 10L, "SALIDA", 3L);
    }

    @Test
    @DisplayName("Filtros opcionales ausentes: todos los artículos y movimientos")
    void filtrosOpcionalesAusentes_usaDefectos() {
        when(service.movimientosStock(any(), any(), any(), any(), any(), any()))
                .thenReturn(respuesta());

        proveedor.obtener(Map.of("desde", "2026-11-01", "hasta", "2026-11-30"));

        verify(service).movimientosStock(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                "", null, "", null);
    }

    @Test
    @DisplayName("Rango ausente o id inválido: 400 y no llama al service")
    void parametrosInvalidos_lanzaDatosInvalidos() {
        assertThatThrownBy(() -> proveedor.obtener(Map.of("hasta", "2026-11-30")))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> proveedor.obtener(Map.of(
                "desde", "2026-11-01", "hasta", "2026-11-30", "id_proveedor", "abc")))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El parámetro 'id_proveedor' tiene un formato inválido");
        verify(service, never()).movimientosStock(any(), any(), any(), any(), any(), any());
    }
}

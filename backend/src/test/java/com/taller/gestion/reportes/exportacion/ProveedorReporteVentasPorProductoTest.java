package com.taller.gestion.reportes.exportacion;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.ReporteVentasPorProductoDTO;
import com.taller.gestion.reportes.service.ReporteVentasPorProductoService;
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
class ProveedorReporteVentasPorProductoTest {

    @Mock
    private ReporteVentasPorProductoService service;

    @InjectMocks
    private ProveedorReporteVentasPorProducto proveedor;

    @Test
    @DisplayName("Registrado con el id que usa la pantalla")
    void identificadoConElIdDelReporte() {
        assertThat(proveedor.id()).isEqualTo("ventas-por-producto");
    }

    @Test
    @DisplayName("Convierte los filtros de la URL en los parámetros del service")
    void filtrosConParametros_llamaAlService() {
        ReporteVentasPorProductoDTO esperado = new ReporteVentasPorProductoDTO(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), 7L, "Bebidas", 5,
                List.of(), List.of(), null);
        when(service.ventasPorProducto(any(), any(), any(), any(), any())).thenReturn(esperado);

        ReporteExportable obtenido = proveedor.obtener(Map.of(
                "desde", "2026-10-01", "hasta", "2026-10-31",
                "id_producto", "7", "categoria", "Bebidas", "top", "5"));

        assertThat(obtenido).isSameAs(esperado);
        verify(service).ventasPorProducto(LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31), 7L, "Bebidas", 5);
    }

    @Test
    @DisplayName("Filtros vacíos: producto null y top por defecto 10")
    void filtrosVacios_usaDefectos() {
        when(service.ventasPorProducto(any(), any(), any(), any(), any())).thenReturn(null);

        proveedor.obtener(Map.of("desde", "2026-10-01", "hasta", "2026-10-31",
                "id_producto", "", "categoria", "", "top", ""));

        verify(service).ventasPorProducto(LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31), null, "", 10);
    }

    @Test
    @DisplayName("Fecha obligatoria ausente: 400 y no llama al service")
    void sinFecha_lanzaDatosInvalidos() {
        assertThatThrownBy(() -> proveedor.obtener(Map.of("hasta", "2026-10-31")))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El parámetro 'desde' es obligatorio");
        verify(service, never()).ventasPorProducto(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Top inválido: 400 y no llama al service")
    void topInvalido_lanzaDatosInvalidos() {
        assertThatThrownBy(() -> proveedor.obtener(Map.of(
                "desde", "2026-10-01", "hasta", "2026-10-31", "top", "abc")))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El parámetro 'top' debe ser un entero positivo");
        verify(service, never()).ventasPorProducto(any(), any(), any(), any(), any());
    }
}

package com.taller.gestion.reportes.exportacion;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.ReporteComparacionEstacionalDTO;
import com.taller.gestion.reportes.service.ReporteComparacionEstacionalService;
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
class ProveedorReporteComparacionEstacionalTest {

    @Mock
    private ReporteComparacionEstacionalService service;

    @InjectMocks
    private ProveedorReporteComparacionEstacional proveedor;

    private ReporteComparacionEstacionalDTO esperado() {
        return new ReporteComparacionEstacionalDTO(
                List.of(2025, 2026), null, null,
                LocalDate.of(2025, 1, 1), LocalDate.of(2026, 11, 30),
                List.of(), null);
    }

    @Test
    @DisplayName("Registrado con el id que usa la pantalla")
    void identificadoConElIdDelReporte() {
        assertThat(proveedor.id()).isEqualTo("evolucion-ventas");
    }

    @Test
    @DisplayName("Convierte los filtros de la URL en los parámetros del service")
    void filtrosConParametros_llamaAlService() {
        ReporteComparacionEstacionalDTO dto = esperado();
        when(service.comparacionEstacional(any(), any(), any())).thenReturn(dto);

        ReporteExportable obtenido = proveedor.obtener(Map.of(
                "anios", "2025,2026", "id_producto", "7", "categoria", "Bebidas"));

        assertThat(obtenido).isSameAs(dto);
        verify(service).comparacionEstacional("2025,2026", 7L, "Bebidas");
    }

    @Test
    @DisplayName("Filtros vacíos: años por defecto, producto null y categoría vacía")
    void filtrosVacios_usaDefectos() {
        when(service.comparacionEstacional(any(), any(), any())).thenReturn(null);

        proveedor.obtener(Map.of("anios", "", "id_producto", "", "categoria", ""));

        verify(service).comparacionEstacional("", null, "");
    }

    @Test
    @DisplayName("Producto con formato inválido: 400 y no llama al service")
    void productoInvalido_lanzaDatosInvalidos() {
        assertThatThrownBy(() -> proveedor.obtener(Map.of("id_producto", "abc")))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El parámetro 'id_producto' tiene un formato inválido");
        verify(service, never()).comparacionEstacional(any(), any(), any());
    }
}

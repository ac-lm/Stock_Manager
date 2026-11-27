package com.taller.gestion.reportes.exportacion;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.ReporteStockActualDTO;
import com.taller.gestion.reportes.dto.TotalesStockActualDTO;
import com.taller.gestion.reportes.service.ReporteStockActualService;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProveedorReporteStockActualTest {

    @Mock
    private ReporteStockActualService service;

    @InjectMocks
    private ProveedorReporteStockActual proveedor;

    private ReporteStockActualDTO respuesta() {
        return new ReporteStockActualDTO(
                LocalDate.of(2026, 11, 26), LocalDate.of(2026, 11, 26),
                "TODOS", null, false, List.of(), List.of(),
                new TotalesStockActualDTO(0, 0));
    }

    @Test
    @DisplayName("Registrado con el id que usa la pantalla")
    void identificadoConElIdDelReporte() {
        assertThat(proveedor.id()).isEqualTo("stock-actual");
    }

    @Test
    @DisplayName("Convierte los filtros de la URL en los parámetros del service")
    void filtrosConParametros_llamaAlService() {
        ReporteStockActualDTO esperado = respuesta();
        when(service.stockActual(any(), any(), anyBoolean())).thenReturn(esperado);

        ReporteExportable obtenido = proveedor.obtener(Map.of(
                "tipo", "INSUMO", "categoria", "", "solo_bajo_minimo", "true"));

        assertThat(obtenido).isSameAs(esperado);
        // ParametrosReporte devuelve "" para la categoría ausente; el service la normaliza a null.
        verify(service).stockActual("INSUMO", "", true);
    }

    @Test
    @DisplayName("Filtros ausentes: todos los tipos, todas las categorías y sin acotar")
    void filtrosVacios_usaDefectos() {
        when(service.stockActual(any(), any(), anyBoolean())).thenReturn(respuesta());

        proveedor.obtener(Map.of());

        verify(service).stockActual("", "", false);
    }

    @Test
    @DisplayName("Booleano inválido: 400 y no llama al service")
    void booleanoInvalido_lanzaDatosInvalidos() {
        assertThatThrownBy(() -> proveedor.obtener(Map.of("solo_bajo_minimo", "quizas")))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El parámetro 'solo_bajo_minimo' tiene un formato inválido");
        verify(service, never()).stockActual(any(), any(), anyBoolean());
    }
}

package com.taller.gestion.incidencias.service;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.common.exception.RecursoDuplicadoException;
import com.taller.gestion.common.exception.RecursoNoEncontradoException;
import com.taller.gestion.incidencias.dto.TipoIncidenciaCreateDTO;
import com.taller.gestion.incidencias.dto.TipoIncidenciaResponseDTO;
import com.taller.gestion.incidencias.dto.TipoIncidenciaUpdateDTO;
import com.taller.gestion.incidencias.entity.TipoIncidencia;
import com.taller.gestion.incidencias.repository.TipoIncidenciaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TipoIncidenciaServiceTest {

    @Mock
    private TipoIncidenciaRepository tipoIncidenciaRepository;

    @InjectMocks
    private TipoIncidenciaService tipoIncidenciaService;

    @Test
    void crearConNombreVacioLanzaError() {
        assertThatThrownBy(() -> tipoIncidenciaService.crearTipo(
                new TipoIncidenciaCreateDTO("   ")))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("obligatorio");

        verifyNoInteractions(tipoIncidenciaRepository);
    }

    @Test
    void crearConNombreDuplicadoLanzaConflicto() {
        when(tipoIncidenciaRepository.existeNombre("Rotura", null)).thenReturn(true);

        assertThatThrownBy(() -> tipoIncidenciaService.crearTipo(
                new TipoIncidenciaCreateDTO("Rotura")))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessageContaining("Ya existe");

        verify(tipoIncidenciaRepository, never()).save(any());
    }

    @Test
    void crearTipoLoGuardaActivo() {
        when(tipoIncidenciaRepository.existeNombre("Rotura", null)).thenReturn(false);
        when(tipoIncidenciaRepository.save(any(TipoIncidencia.class))).thenAnswer(invocation -> {
            TipoIncidencia guardado = invocation.getArgument(0);
            guardado.setId_tipo_incidencia(10L);
            return guardado;
        });

        TipoIncidenciaResponseDTO response = tipoIncidenciaService.crearTipo(
                new TipoIncidenciaCreateDTO("Rotura"));

        assertThat(response.id_tipo_incidencia()).isEqualTo(10L);
        assertThat(response.nombre()).isEqualTo("Rotura");
        assertThat(response.activo()).isTrue();
    }

    @Test
    void actualizarConNombreDuplicadoLanzaConflicto() {
        when(tipoIncidenciaRepository.findById(10L)).thenReturn(Optional.of(tipo(10L, "Rotura")));
        when(tipoIncidenciaRepository.existeNombre("Pérdida", 10L)).thenReturn(true);

        assertThatThrownBy(() -> tipoIncidenciaService.actualizarTipo(
                10L, new TipoIncidenciaUpdateDTO("Pérdida", null)))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessageContaining("Ya existe");
    }

    @Test
    void eliminarTipoHaceBajaLogica() {
        TipoIncidencia tipo = tipo(10L, "Rotura");
        when(tipoIncidenciaRepository.findById(10L)).thenReturn(Optional.of(tipo));
        when(tipoIncidenciaRepository.save(any(TipoIncidencia.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        tipoIncidenciaService.eliminarTipo(10L);

        assertThat(tipo.isActivo()).isFalse();
        verify(tipoIncidenciaRepository).save(tipo);
    }

    @Test
    void obtenerActivoPorIdConTipoInactivoLanzaError() {
        TipoIncidencia tipo = tipo(10L, "Rotura");
        tipo.setActivo(false);
        when(tipoIncidenciaRepository.findById(10L)).thenReturn(Optional.of(tipo));

        assertThatThrownBy(() -> tipoIncidenciaService.obtenerActivoPorId(10L))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("inactivo");
    }

    @Test
    void obtenerPorIdInexistenteLanzaNoEncontrado() {
        when(tipoIncidenciaRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tipoIncidenciaService.obtenerPorId(404L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("404");
    }

    private TipoIncidencia tipo(Long id, String nombre) {
        TipoIncidencia tipo = new TipoIncidencia();
        tipo.setId_tipo_incidencia(id);
        tipo.setNombre(nombre);
        tipo.setActivo(true);
        return tipo;
    }
}

package com.taller.gestion.costos.service;

import com.taller.gestion.common.exception.OperacionNoPermitidaException;
import com.taller.gestion.common.exception.RecursoDuplicadoException;
import com.taller.gestion.common.exception.RecursoNoEncontradoException;
import com.taller.gestion.costos.dto.TipoCostoCreateDTO;
import com.taller.gestion.costos.dto.TipoCostoResponseDTO;
import com.taller.gestion.costos.dto.TipoCostoUpdateDTO;
import com.taller.gestion.costos.entity.TipoCosto;
import com.taller.gestion.costos.repository.TipoCostoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TipoCostoServiceTest {

    @Mock
    private TipoCostoRepository tipoCostoRepository;

    @InjectMocks
    private TipoCostoService tipoCostoService;

    private TipoCosto tipo(Long id, String nombre, boolean activo, boolean reservado) {
        TipoCosto tipo = new TipoCosto();
        tipo.setId_tipo_costo(id);
        tipo.setNombre(nombre);
        tipo.setActivo(activo);
        tipo.setReservado(reservado);
        return tipo;
    }

    // ============================ LISTADO ============================

    @Test
    @DisplayName("Listar: por defecto pide solo los activos y mapea la respuesta")
    void listar_soloActivos() {
        when(tipoCostoRepository.listar(false))
                .thenReturn(List.of(tipo(1L, "Alquiler", true, false)));

        List<TipoCostoResponseDTO> resultado = tipoCostoService.listar(false);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).id_tipo_costo()).isEqualTo(1L);
        assertThat(resultado.get(0).nombre()).isEqualTo("Alquiler");
        assertThat(resultado.get(0).activo()).isTrue();
        assertThat(resultado.get(0).reservado()).isFalse();
        verify(tipoCostoRepository).listar(false);
    }

    @Test
    @DisplayName("Listar: con incluirInactivos pide también los desactivados")
    void listar_incluyeInactivos() {
        when(tipoCostoRepository.listar(true)).thenReturn(List.of());

        assertThat(tipoCostoService.listar(true)).isEmpty();
        verify(tipoCostoRepository).listar(true);
    }

    // ============================ ALTA ============================

    @Test
    @DisplayName("Alta: crea el tipo activo y no reservado")
    void crear_valido() {
        when(tipoCostoRepository.existsByNombreNormalizado(anyString())).thenReturn(false);
        when(tipoCostoRepository.save(any(TipoCosto.class)))
                .thenAnswer(inv -> {
                    TipoCosto guardado = inv.getArgument(0);
                    guardado.setId_tipo_costo(5L);
                    return guardado;
                });

        TipoCostoResponseDTO resultado = tipoCostoService.crear(new TipoCostoCreateDTO("  Alquiler  "));

        ArgumentCaptor<TipoCosto> captor = ArgumentCaptor.forClass(TipoCosto.class);
        verify(tipoCostoRepository).save(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Alquiler");
        assertThat(captor.getValue().isActivo()).isTrue();
        assertThat(captor.getValue().isReservado()).isFalse();
        assertThat(resultado.id_tipo_costo()).isEqualTo(5L);
    }

    @Test
    @DisplayName("Alta: rechaza un nombre duplicado (normalizado)")
    void crear_duplicado() {
        when(tipoCostoRepository.existsByNombreNormalizado("alquiler")).thenReturn(true);

        assertThatThrownBy(() -> tipoCostoService.crear(new TipoCostoCreateDTO("Alquiler")))
                .isInstanceOf(RecursoDuplicadoException.class);
        verify(tipoCostoRepository, never()).save(any());
    }

    // ============================ EDICIÓN ============================

    @Test
    @DisplayName("Edición: cambia el nombre de un tipo editable")
    void actualizar_valido() {
        TipoCosto tipo = tipo(1L, "Alquiler", true, false);
        when(tipoCostoRepository.findById(1L)).thenReturn(Optional.of(tipo));
        when(tipoCostoRepository.existsByNombreNormalizadoAndIdNot("alquiler de local", 1L))
                .thenReturn(false);

        TipoCostoResponseDTO resultado = tipoCostoService.actualizar(
                1L, new TipoCostoUpdateDTO("Alquiler de local"));

        assertThat(tipo.getNombre()).isEqualTo("Alquiler de local");
        assertThat(resultado.nombre()).isEqualTo("Alquiler de local");
    }

    @Test
    @DisplayName("Edición: rechaza un id inexistente")
    void actualizar_inexistente() {
        when(tipoCostoRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tipoCostoService.actualizar(1L, new TipoCostoUpdateDTO("Otro")))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("Edición: rechaza el tipo reservado del sistema")
    void actualizar_reservado() {
        when(tipoCostoRepository.findById(1L))
                .thenReturn(Optional.of(tipo(1L, "Insumos", true, true)));

        assertThatThrownBy(() -> tipoCostoService.actualizar(1L, new TipoCostoUpdateDTO("Otro")))
                .isInstanceOf(OperacionNoPermitidaException.class);
        verify(tipoCostoRepository, never()).existsByNombreNormalizadoAndIdNot(anyString(), anyLong());
    }

    @Test
    @DisplayName("Edición: rechaza un nombre ya usado por otro tipo")
    void actualizar_duplicado() {
        when(tipoCostoRepository.findById(1L))
                .thenReturn(Optional.of(tipo(1L, "Alquiler", true, false)));
        when(tipoCostoRepository.existsByNombreNormalizadoAndIdNot("luz", 1L)).thenReturn(true);

        assertThatThrownBy(() -> tipoCostoService.actualizar(1L, new TipoCostoUpdateDTO("Luz")))
                .isInstanceOf(RecursoDuplicadoException.class);
    }

    // ============================ ESTADO ============================

    @Test
    @DisplayName("Estado: desactiva un tipo editable")
    void cambiarEstado_desactiva() {
        TipoCosto tipo = tipo(1L, "Alquiler", true, false);
        when(tipoCostoRepository.findById(1L)).thenReturn(Optional.of(tipo));

        TipoCostoResponseDTO resultado = tipoCostoService.cambiarEstado(1L, false);

        assertThat(tipo.isActivo()).isFalse();
        assertThat(resultado.activo()).isFalse();
    }

    @Test
    @DisplayName("Estado: rechaza cambiar el tipo reservado del sistema")
    void cambiarEstado_reservado() {
        when(tipoCostoRepository.findById(1L))
                .thenReturn(Optional.of(tipo(1L, "Insumos", true, true)));

        assertThatThrownBy(() -> tipoCostoService.cambiarEstado(1L, false))
                .isInstanceOf(OperacionNoPermitidaException.class);
    }

    @Test
    @DisplayName("Estado: rechaza un id inexistente")
    void cambiarEstado_inexistente() {
        when(tipoCostoRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tipoCostoService.cambiarEstado(1L, true))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}

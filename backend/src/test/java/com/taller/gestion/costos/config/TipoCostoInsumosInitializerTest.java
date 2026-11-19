package com.taller.gestion.costos.config;

import com.taller.gestion.costos.entity.TipoCosto;
import com.taller.gestion.costos.repository.TipoCostoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TipoCostoInsumosInitializerTest {

    @Mock
    private TipoCostoRepository tipoCostoRepository;

    @InjectMocks
    private TipoCostoInsumosInitializer initializer;

    @Test
    @DisplayName("Si no existe, crea Insumos activo, reservado y con el nombre normalizado")
    void crear_insumoReservado() {
        when(tipoCostoRepository.existsByNombreNormalizado("insumos")).thenReturn(false);

        initializer.run(new DefaultApplicationArguments());

        ArgumentCaptor<TipoCosto> captor = ArgumentCaptor.forClass(TipoCosto.class);
        verify(tipoCostoRepository).save(captor.capture());
        TipoCosto guardado = captor.getValue();
        assertThat(guardado.getNombre()).isEqualTo("Insumos");
        assertThat(guardado.getNombreNormalizado()).isEqualTo("insumos");
        assertThat(guardado.isActivo()).isTrue();
        assertThat(guardado.isReservado()).isTrue();
    }

    @Test
    @DisplayName("Si ya existe, no vuelve a guardarlo")
    void crear_yaExiste_noGuarda() {
        when(tipoCostoRepository.existsByNombreNormalizado("insumos")).thenReturn(true);

        initializer.run(new DefaultApplicationArguments());

        verify(tipoCostoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Si otra instancia lo crea al mismo tiempo, el error de unicidad no rompe el arranque")
    void crear_conflictoDeUnicidad_noFalla() {
        when(tipoCostoRepository.existsByNombreNormalizado("insumos")).thenReturn(false);
        when(tipoCostoRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicado"));

        assertThatCode(() -> initializer.run(new DefaultApplicationArguments()))
                .doesNotThrowAnyException();
    }
}

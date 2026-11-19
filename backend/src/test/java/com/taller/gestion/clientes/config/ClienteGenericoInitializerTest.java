package com.taller.gestion.clientes.config;

import com.taller.gestion.clientes.entity.Cliente;
import com.taller.gestion.clientes.entity.TipoCliente;
import com.taller.gestion.clientes.repository.ClienteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteGenericoInitializerTest {

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClienteGenericoInitializer initializer;

    @Test
    @DisplayName("Si no existe, crea el genérico con CUIT nulo y tipo GENERICO")
    void crear_genericoAusente() {
        when(clienteRepository.findByTipoCliente(TipoCliente.GENERICO)).thenReturn(Optional.empty());

        initializer.run(new DefaultApplicationArguments());

        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).save(captor.capture());
        Cliente guardado = captor.getValue();
        assertThat(guardado.getNombre()).isEqualTo("Consumidor final");
        assertThat(guardado.getNombreNormalizado()).isEqualTo("consumidor final");
        assertThat(guardado.getCuit()).isNull();
        assertThat(guardado.getTipoCliente()).isEqualTo(TipoCliente.GENERICO);
        assertThat(guardado.isActivo()).isTrue();
        assertThat(guardado.getEmail()).isNotBlank();
        assertThat(guardado.getDireccion()).isNotBlank();
        assertThat(guardado.getPlazoPagoDias()).isNull();
    }

    @Test
    @DisplayName("Si ya existe, no vuelve a guardarlo")
    void crear_genericoYaExiste_noGuarda() {
        Cliente existente = new Cliente();
        existente.setId_cliente(1L);
        existente.setTipoCliente(TipoCliente.GENERICO);
        when(clienteRepository.findByTipoCliente(TipoCliente.GENERICO))
                .thenReturn(Optional.of(existente));

        initializer.run(new DefaultApplicationArguments());

        verify(clienteRepository, never()).save(any());
    }
}

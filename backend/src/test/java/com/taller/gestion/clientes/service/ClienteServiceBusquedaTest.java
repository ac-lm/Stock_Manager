package com.taller.gestion.clientes.service;

import com.taller.gestion.clientes.dto.ClienteSimilarDTO;
import com.taller.gestion.clientes.entity.Cliente;
import com.taller.gestion.clientes.entity.TipoCliente;
import com.taller.gestion.clientes.repository.ClienteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceBusquedaTest {

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClienteService clienteService;

    private final Pageable pageable = PageRequest.of(0, 10);

    // ============================ buscar ============================

    @Test
    @DisplayName("Buscar un texto con letras: no se usa para buscar por CUIT")
    void buscar_textoConLetras_digitosNoMatchean() {
        when(clienteRepository.buscar(any(), any(), eq(false), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        clienteService.buscar("Taller 2", false, pageable);

        verify(clienteRepository).buscar("Taller 2", "#", false, pageable);
    }

    @Test
    @DisplayName("Buscar un número: se buscan los dígitos sin guiones")
    void buscar_numero_usaDigitos() {
        when(clienteRepository.buscar(any(), any(), eq(false), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        clienteService.buscar("20-1234", false, pageable);

        verify(clienteRepository).buscar("20-1234", "201234", false, pageable);
    }

    @Test
    @DisplayName("Buscar sin texto: lista todo y respeta el filtro de inactivos")
    void buscar_sinTexto_incluyeInactivosSegunFiltro() {
        when(clienteRepository.buscar(any(), any(), eq(true), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        clienteService.buscar(null, true, pageable);

        verify(clienteRepository).buscar("", "", true, pageable);
    }

    // =========================== similares ===========================

    @Test
    @DisplayName("Similares: con menos de 3 letras no consulta la base")
    void similares_textoCorto_devuelveVacio() {
        List<ClienteSimilarDTO> resultado = clienteService.buscarSimilares("pe");

        assertThat(resultado).isEmpty();
        verify(clienteRepository, never()).buscarSimilares(any(), any());
    }

    @Test
    @DisplayName("Similares: normaliza el texto y mapea los resultados")
    void similares_normalizaYMapea() {
        Cliente c = new Cliente();
        c.setId_cliente(7L);
        c.setNombre("José Pérez");
        c.setCuit("20-12345678-6");
        c.setTipo_cliente(TipoCliente.GRAN_CLIENTE);
        c.setActivo(false);
        when(clienteRepository.buscarSimilares(eq("jose perez"), any())).thenReturn(List.of(c));

        List<ClienteSimilarDTO> resultado = clienteService.buscarSimilares("  JOSÉ   Pérez ");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).id_cliente()).isEqualTo(7L);
        assertThat(resultado.get(0).activo()).isFalse();
    }
}
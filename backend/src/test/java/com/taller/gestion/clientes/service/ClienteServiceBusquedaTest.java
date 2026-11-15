package com.taller.gestion.clientes.service;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
}

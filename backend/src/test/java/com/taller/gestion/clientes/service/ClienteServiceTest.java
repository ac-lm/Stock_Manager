package com.taller.gestion.clientes.service;

import com.taller.gestion.clientes.dto.ClienteCreateDTO;
import com.taller.gestion.clientes.dto.ClienteResponseDTO;
import com.taller.gestion.clientes.dto.ClienteUpdateDTO;
import com.taller.gestion.clientes.entity.Cliente;
import com.taller.gestion.clientes.entity.TipoCliente;
import com.taller.gestion.clientes.repository.ClienteRepository;
import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.common.exception.OperacionNoPermitidaException;
import com.taller.gestion.common.exception.RecursoDuplicadoException;
import com.taller.gestion.common.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {
    private static final String CUIT_VALIDO = "20-12345678-6";
    private static final String OTRO_CUIT_VALIDO = "30-71234567-1";
    private static final String CUIT_INVALIDO = "20-12345678-9";

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClienteService clienteService;

    private ClienteCreateDTO crearDto(String cuit, TipoCliente tipo) {
        return new ClienteCreateDTO(cuit, "Panadería Sol", "2262-123456",
                "sol@mail.com", "Calle 1 123", tipo, null);
    }

    private ClienteUpdateDTO actualizarDto(String cuit, TipoCliente tipo) {
        return new ClienteUpdateDTO(cuit, "Panadería Sol", "2262-123456",
                "sol@mail.com", "Calle 1 123", tipo, null);
    }

    private Cliente clienteExistente(Long id, TipoCliente tipo, String cuit) {
        Cliente c = new Cliente();
        c.setId_cliente(id);
        c.setNombre("Cliente Existente");
        c.setEmail("existente@mail.com");
        c.setDireccion("Calle 2 456");
        c.setTipo_cliente(tipo);
        c.setCuit(cuit);
        return c;
    }

    // ============================ ALTA ============================

    @Test
    @DisplayName("Alta válida: guarda el cliente con el CUIT normalizado (con guiones)")
    void alta_valida_normalizaCuit() {
        when(clienteRepository.existsByCuit(CUIT_VALIDO)).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ClienteResponseDTO resultado = clienteService.save(
                crearDto("20123456786", TipoCliente.GRAN_CLIENTE));

        assertThat(resultado.cuit()).isEqualTo(CUIT_VALIDO);
        verify(clienteRepository).save(any(Cliente.class));
    }

    @Test
    @DisplayName("Alta de gran cliente sin CUIT: falla")
    void alta_granClienteSinCuit_falla() {
        assertThatThrownBy(() ->
                clienteService.save(crearDto(null, TipoCliente.GRAN_CLIENTE)))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("obligatorio");

        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Alta de consumidor final sin CUIT: se guarda")
    void alta_consumidorFinalSinCuit_seGuarda() {
        when(clienteRepository.save(any(Cliente.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ClienteResponseDTO resultado = clienteService.save(
                crearDto(null, TipoCliente.CONSUMIDOR_FINAL));

        assertThat(resultado.cuit()).isNull();
        verify(clienteRepository).save(any(Cliente.class));
    }

    @Test
    @DisplayName("Alta con CUIT de dígito verificador incorrecto: falla")
    void alta_cuitInvalido_falla() {
        assertThatThrownBy(() ->
                clienteService.save(crearDto(CUIT_INVALIDO, TipoCliente.GRAN_CLIENTE)))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("no es válido");

        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Alta con CUIT vacío en consumidor final: se guarda con CUIT null")
    void alta_cuitVacio_seGuardaNull() {
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        ClienteResponseDTO resultado = clienteService.save(
                crearDto("   ", TipoCliente.CONSUMIDOR_FINAL));

        assertThat(resultado.cuit()).isNull();
    }

    @Test
    @DisplayName("Alta con CUIT ya existente: falla por duplicado")
    void alta_cuitDuplicado_falla() {
        when(clienteRepository.existsByCuit(CUIT_VALIDO)).thenReturn(true);

        assertThatThrownBy(() ->
                clienteService.save(crearDto(CUIT_VALIDO, TipoCliente.GRAN_CLIENTE)))
                .isInstanceOf(RecursoDuplicadoException.class);

        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Alta con tipo GENERICO: falla (reservado para el sistema)")
    void alta_tipoGenerico_falla() {
        assertThatThrownBy(() ->
                clienteService.save(crearDto(null, TipoCliente.GENERICO)))
                .isInstanceOf(DatosInvalidosException.class);

        verify(clienteRepository, never()).save(any());
    }

    // ========================== MODIFICACIÓN ==========================

    @Test
    @DisplayName("Update conservando el propio CUIT: no da duplicado")
    void update_conservandoSuCuit_noEsDuplicado() {
        Cliente existente = clienteExistente(1L, TipoCliente.GRAN_CLIENTE, CUIT_VALIDO);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(clienteRepository.existsByCuitAndIdNot(CUIT_VALIDO, 1L)).thenReturn(false);

        ClienteResponseDTO resultado = clienteService.update(1L,
                actualizarDto(CUIT_VALIDO, TipoCliente.GRAN_CLIENTE));

        assertThat(resultado.cuit()).isEqualTo(CUIT_VALIDO);
    }

    @Test
    @DisplayName("Update con el CUIT de otro cliente: falla por duplicado")
    void update_cuitDeOtroCliente_falla() {
        Cliente existente = clienteExistente(1L, TipoCliente.GRAN_CLIENTE, CUIT_VALIDO);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(clienteRepository.existsByCuitAndIdNot(OTRO_CUIT_VALIDO, 1L)).thenReturn(true);

        assertThatThrownBy(() -> clienteService.update(1L,
                actualizarDto(OTRO_CUIT_VALIDO, TipoCliente.GRAN_CLIENTE)))
                .isInstanceOf(RecursoDuplicadoException.class);
    }

    @Test
    @DisplayName("Update del cliente genérico: no se permite")
    void update_clienteGenerico_noSePermite() {
        Cliente generico = clienteExistente(1L, TipoCliente.GENERICO, null);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(generico));

        assertThatThrownBy(() -> clienteService.update(1L,
                actualizarDto(null, TipoCliente.CONSUMIDOR_FINAL)))
                .isInstanceOf(OperacionNoPermitidaException.class);
    }

    @Test
    @DisplayName("Update con id inexistente: no encontrado")
    void update_idInexistente_noEncontrado() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.update(99L,
                actualizarDto(null, TipoCliente.CONSUMIDOR_FINAL)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // ======================= DESACTIVAR / REACTIVAR =======================

    @Test
    @DisplayName("Desactivar y reactivar un cliente común cambia 'activo'")
    void cambiarEstado_desactivaYReactiva() {
        Cliente cliente = clienteExistente(1L, TipoCliente.CONSUMIDOR_FINAL, null);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        assertThat(clienteService.cambiarEstado(1L, false).activo()).isFalse();
        assertThat(clienteService.cambiarEstado(1L, true).activo()).isTrue();
    }

    @Test
    @DisplayName("Desactivar el cliente genérico: no se permite")
    void cambiarEstado_clienteGenerico_noSePermite() {
        Cliente generico = clienteExistente(1L, TipoCliente.GENERICO, null);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(generico));

        assertThatThrownBy(() -> clienteService.cambiarEstado(1L, false))
                .isInstanceOf(OperacionNoPermitidaException.class);
    }
}

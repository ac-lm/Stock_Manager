package com.taller.gestion.clientes.repository;

import com.taller.gestion.clientes.entity.Cliente;
import com.taller.gestion.clientes.entity.TipoCliente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // usar Postgres, no H2
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class ClienteRepositoryTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private ClienteRepository clienteRepository;

    private final Pageable pagina = PageRequest.of(0, 10);

    private Cliente guardar(String nombre, String cuit, TipoCliente tipo, boolean activo) {
        Cliente c = new Cliente();
        c.setNombre(nombre);
        c.setCuit(cuit);
        c.setEmail("mail@mail.com");
        c.setDireccion("Calle 1 123");
        c.setTipo_cliente(tipo);
        c.setActivo(activo);
        return clienteRepository.saveAndFlush(c);
    }

    // ============================ buscar ============================

    @Test
    @DisplayName("Buscar por nombre ignora mayúsculas")
    void buscar_porNombre() {
        guardar("Panadería Sol", null, TipoCliente.CONSUMIDOR_FINAL, true);
        guardar("Kiosco Luna", null, TipoCliente.CONSUMIDOR_FINAL, true);

        var resultado = clienteRepository.buscar("SOL", "#", false, pagina);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getNombre()).isEqualTo("Panadería Sol");
    }

    @Test
    @DisplayName("Buscar por CUIT: encuentra aunque se escriba sin guiones")
    void buscar_porCuitSinGuiones() {
        guardar("Panadería Sol", "20-12345678-6", TipoCliente.GRAN_CLIENTE, true);

        var resultado = clienteRepository.buscar("20123456", "20123456", false, pagina);

        assertThat(resultado.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Por defecto solo activos; con el filtro, también inactivos")
    void buscar_filtroInactivos() {
        guardar("Cliente Activo", null, TipoCliente.CONSUMIDOR_FINAL, true);
        guardar("Cliente Inactivo", null, TipoCliente.CONSUMIDOR_FINAL, false);

        assertThat(clienteRepository.buscar("", "", false, pagina).getContent()).hasSize(1);
        assertThat(clienteRepository.buscar("", "", true, pagina).getContent()).hasSize(2);
    }

    // ======================== existsByCuitAndIdNot ========================

    @Test
    @DisplayName("existsByCuitAndIdNot excluye al propio cliente")
    void existsByCuitAndIdNot_excluyeAlPropio() {
        Cliente c = guardar("Panadería Sol", "20-12345678-6", TipoCliente.GRAN_CLIENTE, true);

        assertThat(clienteRepository.existsByCuitAndIdNot("20-12345678-6", c.getId_cliente())).isFalse();
        assertThat(clienteRepository.existsByCuitAndIdNot("20-12345678-6", 999L)).isTrue();
    }

    // =========================== buscarSimilares ===========================

    @Test
    @DisplayName("Similares ignora acentos y mayúsculas")
    void similares_ignoraAcentosYMayusculas() {
        guardar("José Pérez Hnos", null, TipoCliente.CONSUMIDOR_FINAL, true);

        var resultado = clienteRepository.buscarSimilares("jose perez", PageRequest.of(0, 5));

        assertThat(resultado).hasSize(1);
    }

    @Test
    @DisplayName("Similares incluye inactivos y excluye al cliente genérico")
    void similares_incluyeInactivosYExcluyeGenerico() {
        guardar("Perez Inactivo", null, TipoCliente.CONSUMIDOR_FINAL, false);
        guardar("Perez Generico", null, TipoCliente.GENERICO, true);

        var resultado = clienteRepository.buscarSimilares("perez", PageRequest.of(0, 5));

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Perez Inactivo");
    }

    // ============================== unique ==============================

    @Test
    @DisplayName("La base rechaza dos clientes con el mismo CUIT")
    void cuit_unicoEnLaBase() {
        guardar("Cliente Uno", "20-12345678-6", TipoCliente.GRAN_CLIENTE, true);

        assertThatThrownBy(() ->
                guardar("Cliente Dos", "20-12345678-6", TipoCliente.GRAN_CLIENTE, true))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
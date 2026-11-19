package com.taller.gestion.venta.repository;

import com.taller.gestion.clientes.entity.TipoCliente;
import com.taller.gestion.venta.entity.EstadoVenta;
import com.taller.gestion.venta.entity.PrecioTipoCliente;
import com.taller.gestion.venta.entity.Venta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class VentaRepositoryTest {

    @Container
    @ServiceConnection
    // Misma imagen que el docker-compose del proyecto, para detectar diferencias de tipo en SQL.
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private PrecioTipoClienteRepository precioTipoClienteRepository;

    private Venta guardarVenta(Long id_cliente, EstadoVenta estado, LocalDate fecha) {
        Venta venta = new Venta();
        venta.setId_cliente(id_cliente);
        venta.setId_vendedor(5L);
        venta.setFecha(fecha);
        venta.setHora(LocalTime.of(10, 30));
        venta.setTotal(new BigDecimal("100.00"));
        venta.setEstado(estado);
        return ventaRepository.saveAndFlush(venta);
    }

    @Test
    @DisplayName("findByIdProductoAndTipoCliente: encuentra el precio de ese producto y tipo")
    void precioPorProductoYTipo() {
        precioTipoClienteRepository.saveAndFlush(
                new PrecioTipoCliente(null, 10L, TipoCliente.GRAN_CLIENTE, new BigDecimal("80.00")));
        precioTipoClienteRepository.saveAndFlush(
                new PrecioTipoCliente(null, 10L, TipoCliente.CONSUMIDOR_FINAL, new BigDecimal("100.00")));
        precioTipoClienteRepository.saveAndFlush(
                new PrecioTipoCliente(null, 11L, TipoCliente.GRAN_CLIENTE, new BigDecimal("50.00")));

        var resultado = precioTipoClienteRepository.findByIdProductoAndTipoCliente(
                10L, TipoCliente.GRAN_CLIENTE);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getPrecio()).isEqualByComparingTo("80.00");
        assertThat(precioTipoClienteRepository.findByIdProductoAndTipoCliente(
                99L, TipoCliente.GRAN_CLIENTE)).isEmpty();
    }

    @Test
    @DisplayName("buscar: combina estado, cliente y rango de fechas")
    void buscar_combinaFiltros() {
        guardarVenta(2L, EstadoVenta.CONFIRMADA, LocalDate.of(2026, 11, 10));
        guardarVenta(2L, EstadoVenta.ANULADA, LocalDate.of(2026, 11, 15));
        guardarVenta(3L, EstadoVenta.CONFIRMADA, LocalDate.of(2026, 11, 20));

        var confirmadasDeDos = ventaRepository.buscar(
                EstadoVenta.CONFIRMADA, 2L,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                PageRequest.of(0, 10));

        assertThat(confirmadasDeDos.getTotalElements()).isEqualTo(1);
        assertThat(confirmadasDeDos.getContent().get(0).getId_cliente()).isEqualTo(2L);

        var todas = ventaRepository.buscar(null, null, null, null, PageRequest.of(0, 10));
        assertThat(todas.getTotalElements()).isEqualTo(3);
    }
}

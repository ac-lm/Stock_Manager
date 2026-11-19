package com.taller.gestion.costos.repository;

import com.taller.gestion.costos.entity.TipoCosto;
import com.taller.gestion.util.TextoUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class TipoCostoRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private TipoCostoRepository tipoCostoRepository;

    private TipoCosto guardar(String nombre, boolean activo) {
        TipoCosto tipo = new TipoCosto();
        tipo.setNombre(nombre);
        tipo.setActivo(activo);
        return tipoCostoRepository.saveAndFlush(tipo);
    }

    @Test
    @DisplayName("existsByNombreNormalizado: ignora mayúsculas y acentos")
    void existsPorNombreNormalizado() {
        guardar("Alquiler", true);
        guardar("Café", true);

        assertThat(tipoCostoRepository.existsByNombreNormalizado("alquiler")).isTrue();
        assertThat(tipoCostoRepository.existsByNombreNormalizado(
                TextoUtil.normalizarParaBusqueda("  ALQUILER  "))).isTrue();
        assertThat(tipoCostoRepository.existsByNombreNormalizado("cafe")).isTrue();
        assertThat(tipoCostoRepository.existsByNombreNormalizado("Luz")).isFalse();
    }

    @Test
    @DisplayName("existsByNombreNormalizadoAndIdNot: excluye el propio tipo")
    void existsPorNombreExcluyendoId() {
        TipoCosto tipo = guardar("Alquiler", true);

        assertThat(tipoCostoRepository.existsByNombreNormalizadoAndIdNot(
                "alquiler", tipo.getId_tipo_costo())).isFalse();
        assertThat(tipoCostoRepository.existsByNombreNormalizadoAndIdNot(
                "alquiler", 999L)).isTrue();
    }

    @Test
    @DisplayName("Listar: por defecto solo activos y ordenados por nombre")
    void listar_filtraYOrdena() {
        guardar("Luz", true);
        guardar("Alquiler", true);
        guardar("Teléfono", false);

        var activos = tipoCostoRepository.listar(false);
        assertThat(activos).extracting(TipoCosto::getNombre)
                .containsExactly("Alquiler", "Luz");
        assertThat(tipoCostoRepository.listar(true)).hasSize(3);
    }
}

package com.taller.gestion.auditoria.repository;

import com.taller.gestion.auditoria.entity.AccionAuditoria;
import com.taller.gestion.auditoria.entity.Auditoria;
import com.taller.gestion.catalogo.entity.Insumo;
import com.taller.gestion.catalogo.repository.InsumoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
class AuditoriaRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    @Autowired
    private InsumoRepository insumoRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @Transactional
    @DisplayName("Guarda y lee un JSONB con valor anterior y nuevo")
    void guardarCambiosJsonRoundTrip() {
        Map<String, Object> cambios = Map.of(
                "nombre", Map.of("anterior", "Viejo", "nuevo", "Nuevo"),
                "stock_actual", Map.of("anterior", 5, "nuevo", 8));

        Auditoria registro = auditoriaRepository.saveAndFlush(new Auditoria(
                LocalDateTime.now(), null, "SISTEMA", AccionAuditoria.MODIFICACION,
                "Insumo", 99L, cambios, null));

        Auditoria leido = auditoriaRepository.findById(registro.getId_auditoria()).orElseThrow();
        assertThat(leido.getCambios()).containsKeys("nombre", "stock_actual");
        assertThat(leido.getCambios().get("stock_actual"))
                .isEqualTo(Map.of("anterior", 5, "nuevo", 8));
    }

    @Test
    @Transactional
    @DisplayName("El listener registra alta, modificación y baja de otra entidad")
    void listenerRegistraCambiosDeEntidades() {
        Insumo insumo = new Insumo();
        insumo.setNombre("Harina");
        insumo.setStockActual(BigDecimal.TEN);
        insumo.setUnidadMedida("kg");
        insumo.setStockMinimo(2);
        insumo.setCosto(new BigDecimal("100.00"));
        insumoRepository.saveAndFlush(insumo);

        List<Auditoria> altas = auditoriaRepository.findAll().stream()
                .filter(a -> a.getAccion() == AccionAuditoria.ALTA && "Insumo".equals(a.getTabla())
                        && insumo.getId_insumo().equals(a.getId_registro()))
                .toList();
        assertThat(altas).hasSize(1);
        assertThat(altas.get(0).getCambios()).containsKey("nombre");
        assertThat(String.valueOf(altas.get(0).getCambios().get("nombre"))).contains("Harina");

        insumo.setStockActual(new BigDecimal("15"));
        insumoRepository.flush();

        List<Auditoria> modificaciones = auditoriaRepository.findAll().stream()
                .filter(a -> a.getAccion() == AccionAuditoria.MODIFICACION
                        && "Insumo".equals(a.getTabla())
                        && insumo.getId_insumo().equals(a.getId_registro()))
                .toList();
        assertThat(modificaciones).hasSize(1);
        assertThat(modificaciones.get(0).getCambios()).containsKey("stock_actual");

        insumoRepository.delete(insumo);
        insumoRepository.flush();

        List<Auditoria> bajas = auditoriaRepository.findAll().stream()
                .filter(a -> a.getAccion() == AccionAuditoria.BAJA && "Insumo".equals(a.getTabla())
                        && insumo.getId_insumo().equals(a.getId_registro()))
                .toList();
        assertThat(bajas).hasSize(1);
        assertThat(String.valueOf(bajas.get(0).getCambios().get("nombre"))).contains("Harina");
    }

    @Test
    @Transactional
    @DisplayName("El login correcto queda registrado sin tocar el módulo usuario")
    void listenerRegistraLogin() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@taller.local\",\"password\":\"Admin1234\"}"))
                .andExpect(status().isOk());

        assertThat(auditoriaRepository.findAll()).anySatisfy(registro -> {
            assertThat(registro.getAccion()).isEqualTo(AccionAuditoria.LOGIN);
            assertThat(registro.getUsuario()).isEqualTo("admin@taller.local");
        });
    }

    @Test
    @Transactional
    @DisplayName("Los filtros por acción y tabla devuelven solo los registros pedidos")
    void buscarConFiltros() {
        auditoriaRepository.saveAndFlush(new Auditoria(
                LocalDateTime.now(), null, "SISTEMA", AccionAuditoria.AJUSTE_STOCK,
                "Movimiento_Stock", 3L, Map.of(), "Ajuste de prueba"));
        auditoriaRepository.saveAndFlush(new Auditoria(
                LocalDateTime.now(), null, "SISTEMA", AccionAuditoria.LOGIN,
                "Usuario", 1L, Map.of(), "Inicio de sesión"));

        Page<Auditoria> resultado = auditoriaRepository.buscar(
                null, AccionAuditoria.AJUSTE_STOCK, "stock", null, null, PageRequest.of(0, 10));

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).getTabla()).isEqualTo("Movimiento_Stock");
    }

    @Test
    @Transactional
    @DisplayName("La consulta funciona sin ningún filtro (parámetros null)")
    void buscarSinFiltros() {
        auditoriaRepository.saveAndFlush(new Auditoria(
                LocalDateTime.now(), null, "SISTEMA", AccionAuditoria.OTRO,
                null, null, Map.of(), "Registro sin tabla"));

        Page<Auditoria> resultado = auditoriaRepository.buscar(
                null, null, null, null, null, PageRequest.of(0, 10));

        assertThat(resultado.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(resultado.getContent()).anySatisfy(registro ->
                assertThat(registro.getDetalle()).isEqualTo("Registro sin tabla"));
    }
}

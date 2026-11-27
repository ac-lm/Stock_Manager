package com.taller.gestion.costos.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taller.gestion.auditoria.entity.AccionAuditoria;
import com.taller.gestion.auditoria.entity.Auditoria;
import com.taller.gestion.auditoria.repository.AuditoriaRepository;
import com.taller.gestion.costos.dto.CostoCreateDTO;
import com.taller.gestion.costos.dto.CostoUpdateDTO;
import com.taller.gestion.usuario.dto.UsuarioResponseDTO;
import com.taller.gestion.usuario.entity.Rol;
import com.taller.gestion.usuario.entity.Usuario;
import com.taller.gestion.usuario.service.UsuarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CST-08: la auditoría de monto la escribe el listener global de Hibernate, no CostoService.
 * Se verifica de punta a punta: el PUT confirma su transacción y el dirty checking dispara el
 * evento de auditoría con el usuario de la sesión.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
@WithMockUser(username = CostoAuditoriaIntegrationTest.EMAIL, roles = "ADMINISTRADOR")
class CostoAuditoriaIntegrationTest {

    static final String EMAIL = "ana.costo@taller.local";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    @MockitoBean
    private UsuarioService usuarioService;

    private Long idUsuario;
    private Long idTipo;
    private Long idCosto;

    @BeforeEach
    void prepararDatos() {
        // El listener consulta el usuario dentro del flush; se evita esa consulta para no
        // modificar la cola de acciones de Hibernate mientras se está vaciando.
        when(usuarioService.buscarPorEmail(EMAIL)).thenReturn(new UsuarioResponseDTO(
                931L, "Ana Costo", EMAIL, Rol.ADMINISTRADOR, true));
        when(usuarioService.obtenerAutenticado()).thenReturn(new Usuario(
                931L, "Ana Costo", EMAIL, "hash", Rol.ADMINISTRADOR, true));
        idUsuario = prepararUsuario();
        idTipo = prepararTipo();
    }

    @AfterEach
    void limpiarDatos() {
        if (idCosto != null) {
            jdbcTemplate.update("DELETE FROM Auditoria WHERE tabla = 'Costo' AND id_registro = ?",
                    idCosto);
            jdbcTemplate.update("DELETE FROM Costo WHERE id_costo = ?", idCosto);
        }
        if (idTipo != null) {
            jdbcTemplate.update("DELETE FROM Auditoria WHERE tabla = 'TipoCosto' AND id_registro = ?",
                    idTipo);
            jdbcTemplate.update("DELETE FROM tipo_costo WHERE id_tipo_costo = ?", idTipo);
        }
        if (idUsuario != null) {
            jdbcTemplate.update("DELETE FROM Usuario WHERE id_usuario = ?", idUsuario);
        }
    }

    private Long prepararUsuario() {
        jdbcTemplate.update(
                "INSERT INTO Usuario (id_usuario, nombre, email, password, rol, activo) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                931L, "Ana Costo", EMAIL, "hash", "ADMINISTRADOR", true);
        return 931L;
    }

    private Long prepararTipo() {
        // Se inserta por JDBC para que la auditoría del listener no se mezcle con la del alta
        // del tipo: lo que se quiere probar es la edición del costo.
        jdbcTemplate.update(
                "INSERT INTO tipo_costo (id_tipo_costo, nombre, nombre_normalizado, activo, reservado) "
                        + "VALUES (?, ?, ?, ?, ?)",
                941L, "Alquiler CST-08", "alquiler cst 08", true, false);
        return 941L;
    }

    private CostoCreateDTO alta(Long idTipo) {
        return new CostoCreateDTO(idTipo, "Alquiler local", new BigDecimal("150.00"),
                LocalDate.of(2026, 11, 19), 11, 2026, null);
    }

    private CostoUpdateDTO edicion(Long idTipo) {
        return new CostoUpdateDTO(idTipo, "Alquiler local", new BigDecimal("175.00"),
                LocalDate.of(2026, 11, 20), 11, 2026, "Ajuste por factura corregida");
    }

    private Long darDeAlta(Long idTipo) {
        // El alta se inserta por JDBC para aislar la prueba de la auditoría de ALTA: el listener
        // escribe en la misma transacción y aquí solo se quiere cubrir la edición del monto.
        CostoCreateDTO dto = alta(idTipo);
        jdbcTemplate.update(
                "INSERT INTO costo (id_costo, id_tipo_costo, descripcion, monto, fecha_pago, "
                        + "periodo_mes, periodo_anio, id_usuario, anulado) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                951L, idTipo, dto.descripcion(), dto.monto(), dto.fechaPago(),
                dto.periodoMes(), dto.periodoAnio(), idUsuario, false);
        idCosto = 951L;
        return idCosto;
    }

    private void editar(Long idCosto, CostoUpdateDTO dto) throws Exception {
        mockMvc.perform(put("/api/costos/" + idCosto)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id_costo").value(idCosto))
                .andExpect(jsonPath("$.monto").value(175.00));
    }

    private List<Auditoria> modificaciones(Long idCosto) {
        return auditoriaRepository.findAll().stream()
                .filter(registro -> registro.getAccion() == AccionAuditoria.MODIFICACION
                        && "Costo".equals(registro.getTabla())
                        && idCosto.equals(registro.getId_registro()))
                .toList();
    }

    @Test
    @DisplayName("El cambio de monto se audita con anterior, nuevo, usuario y fecha")
    @SuppressWarnings("unchecked")
    void cambioDeMontoSeAudita() throws Exception {
        Long costo = darDeAlta(idTipo);

        editar(costo, edicion(idTipo));

        List<Auditoria> modificaciones = modificaciones(costo);
        assertThat(modificaciones).hasSize(1);
        Auditoria registro = modificaciones.get(0);
        assertThat(registro.getTabla()).isEqualTo("Costo");
        assertThat(registro.getId_registro()).isEqualTo(costo);
        assertThat(registro.getId_usuario()).isEqualTo(idUsuario);
        assertThat(registro.getUsuario()).isEqualTo(EMAIL);
        assertThat(registro.getFecha_hora()).isNotNull();
        assertThat(registro.getCambios()).containsKey("monto");
        Map<String, Object> monto = (Map<String, Object>) registro.getCambios().get("monto");
        assertThat(new BigDecimal(String.valueOf(monto.get("anterior"))))
                .isEqualByComparingTo("150.00");
        assertThat(new BigDecimal(String.valueOf(monto.get("nuevo"))))
                .isEqualByComparingTo("175.00");
    }

    @Test
    @DisplayName("Guardar el mismo monto no agrega auditoría y la edición sigue respondiendo igual")
    void mismoMontoNoGeneraAuditoria() throws Exception {
        Long costo = darDeAlta(idTipo);

        editar(costo, edicion(idTipo));
        int cantidadDespuesDelCambio = modificaciones(costo).size();
        assertThat(cantidadDespuesDelCambio).isEqualTo(1);

        editar(costo, edicion(idTipo));

        assertThat(modificaciones(costo)).hasSize(cantidadDespuesDelCambio);
    }
}

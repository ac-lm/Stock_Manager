package com.taller.gestion.auditoria.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taller.gestion.catalogo.dto.ProductoCreateDTO;
import com.taller.gestion.usuario.dto.UsuarioCreateDTO;
import com.taller.gestion.usuario.entity.Rol;
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
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regresión del bug que marcaba rollback-only la transacción al auditar un alta: con una
 * sesión autenticada, el listener consultaba el usuario justo después del insert y el
 * auto-flush de Hibernate se llevaba por delante la operación. Acá se comprueba primero
 * que el alta se guardó y después que quedó su fila de auditoría.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
@WithMockUser(username = AltaConAuditoriaIntegrationTest.ADMIN_EMAIL, roles = "ADMINISTRADOR")
class AltaConAuditoriaIntegrationTest {

    static final String ADMIN_EMAIL = "admin.alta@taller.local";
    private static final Long ID_ADMIN = 971L;
    private static final String EMAIL_INEXISTENTE = "fantasma.alta@taller.local";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long idProductoCreado;
    private Long idUsuarioCreado;

    @BeforeEach
    void prepararAdmin() {
        jdbcTemplate.update(
                "INSERT INTO usuario (id_usuario, nombre, email, password, rol, activo) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                ID_ADMIN, "Admin Alta", ADMIN_EMAIL, "hash", "ADMINISTRADOR", true);
    }

    @AfterEach
    void limpiar() {
        if (idProductoCreado != null) {
            jdbcTemplate.update(
                    "DELETE FROM auditoria WHERE tabla = 'Producto' AND id_registro = ?",
                    idProductoCreado);
            jdbcTemplate.update("DELETE FROM producto WHERE id_producto = ?", idProductoCreado);
        }
        if (idUsuarioCreado != null) {
            jdbcTemplate.update(
                    "DELETE FROM auditoria WHERE tabla = 'Usuario' AND id_registro = ?",
                    idUsuarioCreado);
            jdbcTemplate.update("DELETE FROM usuario WHERE id_usuario = ?", idUsuarioCreado);
        }
        jdbcTemplate.update("DELETE FROM usuario WHERE id_usuario = ?", ID_ADMIN);
        idProductoCreado = null;
        idUsuarioCreado = null;
    }

    @Test
    @DisplayName("Con un administrador logueado, el alta de producto y de usuario se guarda y se audita")
    void altasConAdminGuardanLaOperacionYLaAuditan() throws Exception {
        String nombreProducto = "Producto alta auditoría";
        String emailNuevo = "nuevo.alta@taller.local";

        darDeAltaProducto(nombreProducto);
        darDeAltaUsuario(emailNuevo);

        idProductoCreado = idProducto(nombreProducto);
        idUsuarioCreado = idUsuario(emailNuevo);

        assertThat(filas("SELECT COUNT(*) FROM producto WHERE id_producto = ?", idProductoCreado))
                .isEqualTo(1);
        assertThat(filas("SELECT COUNT(*) FROM usuario WHERE id_usuario = ?", idUsuarioCreado))
                .isEqualTo(1);
        assertThat(idActorDeAlta("Producto", idProductoCreado)).isEqualTo(ID_ADMIN);
        assertThat(idActorDeAlta("Usuario", idUsuarioCreado)).isEqualTo(ID_ADMIN);
        assertThat(actorDeAlta("Producto", idProductoCreado)).isEqualTo(ADMIN_EMAIL);
        assertThat(actorDeAlta("Usuario", idUsuarioCreado)).isEqualTo(ADMIN_EMAIL);
    }

    @Test
    @WithMockUser(username = EMAIL_INEXISTENTE, roles = "ADMINISTRADOR")
    @DisplayName("Un usuario de sesión que no existe en la BD no frena el alta ni deja la operación sin guardar")
    void sesionInexistenteNoFrenaElAlta() throws Exception {
        String nombreProducto = "Producto alta sin actor";
        String emailNuevo = "nuevo.sin.actor@taller.local";

        darDeAltaProducto(nombreProducto);
        darDeAltaUsuario(emailNuevo);

        idProductoCreado = idProducto(nombreProducto);
        idUsuarioCreado = idUsuario(emailNuevo);

        assertThat(filas("SELECT COUNT(*) FROM producto WHERE id_producto = ?", idProductoCreado))
                .isEqualTo(1);
        assertThat(filas("SELECT COUNT(*) FROM usuario WHERE id_usuario = ?", idUsuarioCreado))
                .isEqualTo(1);
        assertThat(idActorDeAlta("Producto", idProductoCreado)).isNull();
        assertThat(idActorDeAlta("Usuario", idUsuarioCreado)).isNull();
        assertThat(actorDeAlta("Producto", idProductoCreado)).isEqualTo(EMAIL_INEXISTENTE);
        assertThat(actorDeAlta("Usuario", idUsuarioCreado)).isEqualTo(EMAIL_INEXISTENTE);
    }

    private void darDeAltaProducto(String nombre) throws Exception {
        mockMvc.perform(post("/api/catalogo/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProductoCreateDTO(
                                nombre, new BigDecimal("125.50"), BigDecimal.ZERO, "Auditoría"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value(nombre));
    }

    private void darDeAltaUsuario(String email) throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UsuarioCreateDTO(
                                "Usuario Nuevo", email, "ClaveSegura123", Rol.VENDEDOR))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email));
    }

    private Long idProducto(String nombre) {
        return jdbcTemplate.queryForObject(
                "SELECT id_producto FROM producto WHERE nombre = ?", Long.class, nombre);
    }

    private Long idUsuario(String email) {
        return jdbcTemplate.queryForObject(
                "SELECT id_usuario FROM usuario WHERE email = ?", Long.class, email);
    }

    private Long idActorDeAlta(String tabla, Long idRegistro) {
        return jdbcTemplate.queryForObject(
                "SELECT id_usuario FROM auditoria "
                        + "WHERE accion = 'ALTA' AND tabla = ? AND id_registro = ?",
                Long.class, tabla, idRegistro);
    }

    private String actorDeAlta(String tabla, Long idRegistro) {
        return jdbcTemplate.queryForObject(
                "SELECT usuario FROM auditoria "
                        + "WHERE accion = 'ALTA' AND tabla = ? AND id_registro = ?",
                String.class, tabla, idRegistro);
    }

    private int filas(String sql, Object... argumentos) {
        return jdbcTemplate.queryForObject(sql, Integer.class, argumentos);
    }
}

package com.taller.gestion.costos.repository;

import com.taller.gestion.costos.entity.Costo;
import com.taller.gestion.costos.entity.TipoCosto;
import com.taller.gestion.usuario.entity.Rol;
import com.taller.gestion.usuario.entity.Usuario;
import com.taller.gestion.usuario.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class CostoRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private CostoRepository costoRepository;

    @Autowired
    private TipoCostoRepository tipoCostoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EntityManager entityManager;

    private final Pageable pagina = PageRequest.of(0, 10);

    private TipoCosto guardarTipo(String nombre) {
        TipoCosto tipo = new TipoCosto();
        tipo.setNombre(nombre);
        return tipoCostoRepository.saveAndFlush(tipo);
    }

    private Usuario guardarUsuario(String email) {
        Usuario usuario = new Usuario();
        usuario.setNombre("Operador");
        usuario.setEmail(email);
        usuario.setPassword("hash");
        usuario.setRol(Rol.VENDEDOR);
        usuario.setActivo(true);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private Costo guardarCosto(TipoCosto tipo, Usuario usuario, String monto,
                               int mes, int anio, boolean anulado, Long idIngreso) {
        Costo costo = new Costo();
        costo.setTipoCosto(tipo);
        costo.setUsuario(usuario);
        costo.setDescripcion("Costo de prueba");
        costo.setMonto(new BigDecimal(monto));
        costo.setFechaPago(LocalDate.of(anio, mes, 15));
        costo.setPeriodoMes(mes);
        costo.setPeriodoAnio(anio);
        costo.setAnulado(anulado);
        if (idIngreso != null) {
            costo.setId_ingreso_mercaderia(idIngreso);
        }
        return costoRepository.saveAndFlush(costo);
    }

    @Test
    @DisplayName("Buscar: filtra por tipo, mes y año")
    void buscar_filtraPorTipoMesAnio() {
        TipoCosto alquiler = guardarTipo("Alquiler");
        TipoCosto luz = guardarTipo("Luz");
        Usuario usuario = guardarUsuario("operador@taller.local");
        guardarCosto(alquiler, usuario, "100.00", 11, 2026, false, null);
        guardarCosto(luz, usuario, "200.00", 11, 2026, false, null);
        guardarCosto(alquiler, usuario, "300.00", 10, 2026, false, null);

        var resultado = costoRepository.buscar(alquiler.getId_tipo_costo(), 11, 2026, false, pagina);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getMonto()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Buscar: por defecto excluye anulados y con el flag los incluye")
    void buscar_filtroAnulados() {
        TipoCosto tipo = guardarTipo("Alquiler");
        Usuario usuario = guardarUsuario("operador@taller.local");
        guardarCosto(tipo, usuario, "100.00", 11, 2026, false, null);
        guardarCosto(tipo, usuario, "200.00", 11, 2026, true, null);

        assertThat(costoRepository.buscar(null, null, null, false, pagina).getContent()).hasSize(1);
        assertThat(costoRepository.buscar(null, null, null, true, pagina).getContent()).hasSize(2);
    }

    @Test
    @DisplayName("Total: ignora los anulados")
    void total_ignoraAnulados() {
        TipoCosto tipo = guardarTipo("Alquiler");
        Usuario usuario = guardarUsuario("operador@taller.local");
        guardarCosto(tipo, usuario, "100.00", 11, 2026, false, null);
        guardarCosto(tipo, usuario, "200.00", 11, 2026, true, null);

        assertThat(costoRepository.total(null, null, null)).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Las dos relaciones con Usuario persisten como FK")
    void relacionesConUsuario_persisten() {
        TipoCosto tipo = guardarTipo("Alquiler");
        Usuario creador = guardarUsuario("creador@taller.local");
        Usuario anulador = guardarUsuario("anulador@taller.local");
        Costo costo = guardarCosto(tipo, creador, "100.00", 11, 2026, false, null);
        costo.setUsuarioAnulacion(anulador);
        costoRepository.saveAndFlush(costo);
        entityManager.clear();

        Costo recargado = costoRepository.findById(costo.getId_costo()).orElseThrow();
        assertThat(recargado.getUsuario().getId_usuario()).isEqualTo(creador.getId_usuario());
        assertThat(recargado.getUsuarioAnulacion().getId_usuario()).isEqualTo(anulador.getId_usuario());
    }
}

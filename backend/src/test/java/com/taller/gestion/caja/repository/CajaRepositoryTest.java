package com.taller.gestion.caja.repository;

import com.taller.gestion.caja.entity.CierreCajaDetalle;
import com.taller.gestion.caja.entity.CierreCajaDetalleId;
import com.taller.gestion.caja.entity.ConceptoMovimientoCaja;
import com.taller.gestion.caja.entity.EstadoSesionCaja;
import com.taller.gestion.caja.entity.MedioPago;
import com.taller.gestion.caja.entity.MovimientoCaja;
import com.taller.gestion.caja.entity.SesionCaja;
import com.taller.gestion.caja.entity.TipoMovimientoCaja;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // usar Postgres, no H2
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class CajaRepositoryTest {

    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private SesionCajaRepository sesionCajaRepository;

    @Autowired
    private MovimientoCajaRepository movimientoCajaRepository;

    @Autowired
    private CierreCajaDetalleRepository cierreCajaDetalleRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private Usuario usuario(String nombre) {
        int n = SECUENCIA.incrementAndGet();
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setEmail("caja" + n + "@taller.local");
        u.setPassword("x");
        u.setRol(Rol.VENDEDOR);
        u.setActivo(true);
        return usuarioRepository.saveAndFlush(u);
    }

    private SesionCaja sesion(Usuario apertura, EstadoSesionCaja estado,
                              LocalDate fechaApertura, LocalTime horaApertura) {
        SesionCaja s = new SesionCaja();
        s.setUsuarioApertura(apertura);
        s.setFechaApertura(fechaApertura);
        s.setHoraApertura(horaApertura);
        s.setMontoInicial(new BigDecimal("1000.00"));
        s.setEstado(estado);
        return sesionCajaRepository.saveAndFlush(s);
    }

    private MovimientoCaja movimiento(SesionCaja sesion, Usuario usuario,
                                      TipoMovimientoCaja tipo, ConceptoMovimientoCaja concepto,
                                      MedioPago medio, String monto,
                                      LocalDate fecha, LocalTime hora) {
        MovimientoCaja m = new MovimientoCaja();
        m.setSesionCaja(sesion);
        m.setUsuario(usuario);
        m.setTipo(tipo);
        m.setConcepto(concepto);
        m.setMedioPago(medio);
        m.setMonto(new BigDecimal(monto));
        m.setFecha(fecha);
        m.setHora(hora);
        return movimientoCajaRepository.saveAndFlush(m);
    }

    // ============================ SESIONES ============================

    @Test
    @DisplayName("Busca la sesión ABIERTA más reciente")
    void buscarSesionAbierta() {
        Usuario ana = usuario("Ana");
        sesion(ana, EstadoSesionCaja.CERRADA, LocalDate.of(2026, 11, 18), LocalTime.of(9, 0));
        SesionCaja abierta = sesion(ana, EstadoSesionCaja.ABIERTA,
                LocalDate.of(2026, 11, 20), LocalTime.of(8, 30));

        SesionCaja encontrada = sesionCajaRepository
                .findFirstByEstadoOrderByFechaAperturaDescHoraAperturaDesc(
                        EstadoSesionCaja.ABIERTA)
                .orElseThrow();

        assertThat(encontrada.getId_sesion()).isEqualTo(abierta.getId_sesion());
    }

    @Test
    @DisplayName("El filtro por usuario matchea apertura o cierre")
    void buscar_porUsuarioAperturaOCierre() {
        Usuario ana = usuario("Ana");
        Usuario beto = usuario("Beto");

        SesionCaja cerradaPorBeto = sesion(ana, EstadoSesionCaja.CERRADA,
                LocalDate.of(2026, 11, 18), LocalTime.of(9, 0));
        cerradaPorBeto.setUsuarioCierre(beto);
        cerradaPorBeto.setFechaCierre(LocalDate.of(2026, 11, 18));
        cerradaPorBeto.setHoraCierre(LocalTime.of(18, 0));
        sesionCajaRepository.saveAndFlush(cerradaPorBeto);

        SesionCaja abiertaPorBeto = sesion(beto, EstadoSesionCaja.ABIERTA,
                LocalDate.of(2026, 11, 20), LocalTime.of(8, 30));

        Page<SesionCaja> deAna = sesionCajaRepository.buscar(
                null, ana.getId_usuario(), null, null, PageRequest.of(0, 10));
        Page<SesionCaja> deBeto = sesionCajaRepository.buscar(
                null, beto.getId_usuario(), null, null, PageRequest.of(0, 10));

        assertThat(deAna.getContent())
                .extracting(SesionCaja::getId_sesion)
                .containsExactly(cerradaPorBeto.getId_sesion());
        assertThat(deBeto.getContent())
                .extracting(SesionCaja::getId_sesion)
                .containsExactlyInAnyOrder(
                        cerradaPorBeto.getId_sesion(), abiertaPorBeto.getId_sesion());
    }

    @Test
    @DisplayName("Filtra por estado y rango de fechas de apertura")
    void buscar_porEstadoYFechas() {
        Usuario ana = usuario("Ana");
        sesion(ana, EstadoSesionCaja.CERRADA, LocalDate.of(2026, 11, 1), LocalTime.of(9, 0));
        sesion(ana, EstadoSesionCaja.CERRADA, LocalDate.of(2026, 11, 15), LocalTime.of(9, 0));
        sesion(ana, EstadoSesionCaja.PENDIENTE_REVISION,
                LocalDate.of(2026, 11, 20), LocalTime.of(9, 0));

        Page<SesionCaja> resultado = sesionCajaRepository.buscar(
                EstadoSesionCaja.CERRADA, null,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 15),
                PageRequest.of(0, 10));

        assertThat(resultado.getContent()).hasSize(2);
        assertThat(resultado.getContent())
                .allMatch(s -> s.getEstado() == EstadoSesionCaja.CERRADA);
    }

    @Test
    @DisplayName("El último cierre incluye sesiones PENDIENTE_REVISION")
    void ultimoCierre_incluyePendienteRevision() {
        Usuario ana = usuario("Ana");
        sesion(ana, EstadoSesionCaja.CERRADA, LocalDate.of(2026, 11, 15), LocalTime.of(9, 0));

        SesionCaja pendiente = sesion(ana, EstadoSesionCaja.PENDIENTE_REVISION,
                LocalDate.of(2026, 11, 19), LocalTime.of(9, 0));
        pendiente.setFechaCierre(LocalDate.of(2026, 11, 19));
        pendiente.setHoraCierre(LocalTime.of(18, 0));
        sesionCajaRepository.saveAndFlush(pendiente);

        SesionCaja encontrada = sesionCajaRepository
                .buscarUltimoCierre(
                        List.of(EstadoSesionCaja.CERRADA,
                                EstadoSesionCaja.PENDIENTE_REVISION))
                .orElseThrow();

        assertThat(encontrada.getId_sesion()).isEqualTo(pendiente.getId_sesion());
    }

    // ============================ MOVIMIENTOS ============================

    @Test
    @DisplayName("Suma ingresos y egresos por tipo y medio")
    void sumarPorTipoYMedio() {
        Usuario ana = usuario("Ana");
        SesionCaja sesion = sesion(ana, EstadoSesionCaja.ABIERTA,
                LocalDate.of(2026, 11, 20), LocalTime.of(8, 0));

        movimiento(sesion, ana, TipoMovimientoCaja.INGRESO, ConceptoMovimientoCaja.VENTA,
                MedioPago.EFECTIVO, "700.00", LocalDate.of(2026, 11, 20), LocalTime.of(9, 0));
        movimiento(sesion, ana, TipoMovimientoCaja.INGRESO, ConceptoMovimientoCaja.VENTA,
                MedioPago.EFECTIVO, "300.00", LocalDate.of(2026, 11, 20), LocalTime.of(10, 0));
        movimiento(sesion, ana, TipoMovimientoCaja.EGRESO, ConceptoMovimientoCaja.RETIRO,
                MedioPago.EFECTIVO, "200.00", LocalDate.of(2026, 11, 20), LocalTime.of(11, 0));
        movimiento(sesion, ana, TipoMovimientoCaja.EGRESO, ConceptoMovimientoCaja.GASTO,
                MedioPago.TRANSFERENCIA, "500.00", LocalDate.of(2026, 11, 20), LocalTime.of(12, 0));

        assertThat(movimientoCajaRepository.sumarPorTipoYMedio(
                sesion.getId_sesion(), MedioPago.EFECTIVO, TipoMovimientoCaja.INGRESO))
                .isEqualByComparingTo("1000.00");
        assertThat(movimientoCajaRepository.sumarPorTipoYMedio(
                sesion.getId_sesion(), MedioPago.EFECTIVO, TipoMovimientoCaja.EGRESO))
                .isEqualByComparingTo("200.00");
        assertThat(movimientoCajaRepository.sumarPorTipoYMedio(
                sesion.getId_sesion(), MedioPago.TRANSFERENCIA, TipoMovimientoCaja.EGRESO))
                .isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("Sin movimientos, la suma es cero y no nula")
    void sumarSinMovimientos_devuelveCero() {
        Usuario ana = usuario("Ana");
        SesionCaja sesion = sesion(ana, EstadoSesionCaja.ABIERTA,
                LocalDate.of(2026, 11, 20), LocalTime.of(8, 0));

        BigDecimal suma = movimientoCajaRepository.sumarPorTipoYMedio(
                sesion.getId_sesion(), MedioPago.EFECTIVO, TipoMovimientoCaja.INGRESO);

        assertThat(suma).isNotNull();
        assertThat(suma).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("existePorCosto: detecta el egreso vinculado y no otros costos (CST-05)")
    void existePorCosto() {
        Usuario ana = usuario("Ana");
        SesionCaja sesion = sesion(ana, EstadoSesionCaja.ABIERTA,
                LocalDate.of(2026, 11, 20), LocalTime.of(8, 0));

        MovimientoCaja egreso = movimiento(sesion, ana, TipoMovimientoCaja.EGRESO,
                ConceptoMovimientoCaja.GASTO, MedioPago.TRANSFERENCIA, "500.00",
                LocalDate.of(2026, 11, 20), LocalTime.of(11, 0));
        egreso.setId_costo(10L);
        movimientoCajaRepository.saveAndFlush(egreso);

        assertThat(movimientoCajaRepository.existePorCosto(10L)).isTrue();
        assertThat(movimientoCajaRepository.existePorCosto(99L)).isFalse();
    }

    @Test
    @DisplayName("Lista los movimientos del más nuevo al más viejo")
    void listarMovimientos_descendente() {
        Usuario ana = usuario("Ana");
        SesionCaja sesion = sesion(ana, EstadoSesionCaja.ABIERTA,
                LocalDate.of(2026, 11, 20), LocalTime.of(8, 0));

        MovimientoCaja primero = movimiento(sesion, ana, TipoMovimientoCaja.INGRESO,
                ConceptoMovimientoCaja.VENTA, MedioPago.EFECTIVO, "100.00",
                LocalDate.of(2026, 11, 20), LocalTime.of(9, 0));
        MovimientoCaja ultimo = movimiento(sesion, ana, TipoMovimientoCaja.EGRESO,
                ConceptoMovimientoCaja.GASTO, MedioPago.EFECTIVO, "50.00",
                LocalDate.of(2026, 11, 20), LocalTime.of(15, 0));

        List<MovimientoCaja> movimientos =
                movimientoCajaRepository.listarPorSesion(sesion.getId_sesion());

        assertThat(movimientos).hasSize(2);
        assertThat(movimientos.get(0).getId_movimiento_caja())
                .isEqualTo(ultimo.getId_movimiento_caja());
        assertThat(movimientos.get(1).getId_movimiento_caja())
                .isEqualTo(primero.getId_movimiento_caja());
    }

    // ============================ DETALLE DE CIERRE ============================

    @Test
    @DisplayName("Persiste el detalle con clave compuesta (sesión, medio)")
    void detalle_claveCompuesta() {
        Usuario ana = usuario("Ana");
        SesionCaja sesion = sesion(ana, EstadoSesionCaja.CERRADA,
                LocalDate.of(2026, 11, 20), LocalTime.of(8, 0));

        cierreCajaDetalleRepository.saveAndFlush(detalle(sesion, MedioPago.EFECTIVO,
                "1000.00", "950.00", "-50.00"));
        cierreCajaDetalleRepository.saveAndFlush(detalle(sesion, MedioPago.TRANSFERENCIA,
                "2000.00", "2000.00", "0.00"));

        entityManager.flush();
        entityManager.clear();

        CierreCajaDetalle efectivo = cierreCajaDetalleRepository
                .buscarPorSesionYMedio(sesion.getId_sesion(), MedioPago.EFECTIVO)
                .orElseThrow();

        assertThat(efectivo.getId().getId_sesion()).isEqualTo(sesion.getId_sesion());
        assertThat(efectivo.getId().getMedioPago()).isEqualTo(MedioPago.EFECTIVO);
        assertThat(efectivo.getTotalDeclarado()).isEqualByComparingTo("950.00");
        assertThat(efectivo.getDiferencia()).isEqualByComparingTo("-50.00");

        List<CierreCajaDetalle> detalles =
                cierreCajaDetalleRepository.listarPorSesion(sesion.getId_sesion());
        assertThat(detalles).hasSize(2);
        assertThat(detalles).extracting(d -> d.getId().getMedioPago())
                .containsExactly(MedioPago.EFECTIVO, MedioPago.TRANSFERENCIA);
    }

    private CierreCajaDetalle detalle(SesionCaja sesion, MedioPago medio,
                                      String esperado, String declarado, String diferencia) {
        CierreCajaDetalle d = new CierreCajaDetalle();
        d.setId(new CierreCajaDetalleId(sesion.getId_sesion(), medio));
        d.setSesionCaja(sesion);
        d.setTotalEsperado(new BigDecimal(esperado));
        d.setTotalDeclarado(new BigDecimal(declarado));
        d.setDiferencia(new BigDecimal(diferencia));
        return d;
    }

    // ============================ BLOQUEO ============================

    @Test
    @DisplayName("El advisory lock de apertura se puede tomar en la transacción")
    void advisoryLock() {
        assertThatCode(() -> jdbcTemplate.execute(
                "SELECT pg_advisory_xact_lock(7401)")).doesNotThrowAnyException();
    }
}

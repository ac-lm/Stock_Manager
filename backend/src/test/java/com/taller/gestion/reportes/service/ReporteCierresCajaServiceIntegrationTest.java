package com.taller.gestion.reportes.service;

import com.taller.gestion.caja.entity.EstadoSesionCaja;
import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.CierreCajaFilaDTO;
import com.taller.gestion.reportes.dto.ReporteCierresCajaDTO;
import com.taller.gestion.reportes.dto.ValoresMedioPagoCierreDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
class ReporteCierresCajaServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ReporteCierresCajaService service;

    private record Usuarios(long apertura, long cierre, long inactivo) {
    }

    private long guardarUsuario(long id, String nombre, boolean activo) {
        jdbcTemplate.update(
                "INSERT INTO Usuario (id_usuario, nombre, email, password, rol, activo) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                id, nombre, "u" + id + "@taller.local",
                "$2a$10$hashDePruebaNoUsadoParaLogin", "VENDEDOR", activo);
        return id;
    }

    private long guardarSesion(LocalDate fecha, EstadoSesionCaja estado, Long idCierre,
                               String montoInicial) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO Sesion_Caja (id_usuario_apertura, id_usuario_cierre, fecha_apertura, "
                        + "hora_apertura, fecha_cierre, hora_cierre, monto_inicial, estado) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id_sesion",
                Long.class,
                201L, idCierre, Date.valueOf(fecha), Time.valueOf(LocalTime.of(9, 0)),
                idCierre == null ? null : Date.valueOf(fecha),
                idCierre == null ? null : Time.valueOf(LocalTime.of(17, 0)),
                new BigDecimal(montoInicial), estado.name());
    }

    private void guardarDetalle(long idSesion, String medio, String esperado,
                                String declarado, String diferencia) {
        jdbcTemplate.update(
                "INSERT INTO Cierre_Caja_Detalle (id_sesion, medio_pago, total_esperado, "
                        + "total_declarado, diferencia) VALUES (?, ?, ?, ?, ?)",
                idSesion, medio, new BigDecimal(esperado), new BigDecimal(declarado),
                new BigDecimal(diferencia));
    }

    private void guardarMovimiento(long idSesion, String tipo, String concepto,
                                   String medio, String monto) {
        jdbcTemplate.update(
                "INSERT INTO Movimiento_Caja (id_sesion_caja, id_usuario, tipo, concepto, monto, "
                        + "medio_pago, fecha, hora) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                idSesion, 201L, tipo, concepto, new BigDecimal(monto), medio,
                Date.valueOf(LocalDate.of(2026, 10, 1)), Time.valueOf(LocalTime.of(10, 0)));
    }

    /** Dos sesiones cerradas en el rango, una abierta y una fuera del rango. */
    private Usuarios cargarDatos() {
        guardarUsuario(201L, "Ana Abre", true);
        guardarUsuario(202L, "Luis Cierra", true);
        guardarUsuario(203L, "Viejo Cierre", false);

        long primera = guardarSesion(LocalDate.of(2026, 10, 1), EstadoSesionCaja.CERRADA,
                202L, "100.00");
        guardarDetalle(primera, "EFECTIVO", "1000.00", "990.00", "-10.00");
        guardarDetalle(primera, "TRANSFERENCIA", "500.00", "500.00", "0.00");
        guardarMovimiento(primera, "INGRESO", "VENTA", "EFECTIVO", "1000.00");
        guardarMovimiento(primera, "INGRESO", "VENTA", "TRANSFERENCIA", "500.00");
        guardarMovimiento(primera, "EGRESO", "ANULACION", "EFECTIVO", "20.00");
        guardarMovimiento(primera, "EGRESO", "RETIRO", "EFECTIVO", "30.00");
        guardarMovimiento(primera, "EGRESO", "GASTO", "TRANSFERENCIA", "5.00");

        long pendiente = guardarSesion(LocalDate.of(2026, 10, 2),
                EstadoSesionCaja.PENDIENTE_REVISION, 203L, "50.00");
        guardarDetalle(pendiente, "EFECTIVO", "200.00", "210.00", "10.00");
        guardarDetalle(pendiente, "TRANSFERENCIA", "0.00", "0.00", "0.00");
        guardarMovimiento(pendiente, "INGRESO", "VENTA", "EFECTIVO", "200.00");

        guardarSesion(LocalDate.of(2026, 10, 3), EstadoSesionCaja.ABIERTA, null, "10.00");
        long fuera = guardarSesion(LocalDate.of(2026, 11, 1), EstadoSesionCaja.CERRADA,
                202L, "999.00");
        guardarDetalle(fuera, "EFECTIVO", "9999.00", "9999.00", "0.00");

        return new Usuarios(201L, 202L, 203L);
    }

    private ReporteCierresCajaDTO reporte() {
        return service.cierresCaja(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                null, "TODOS", "TODOS");
    }

    private CierreCajaFilaDTO fila(ReporteCierresCajaDTO reporte, LocalDate fecha) {
        return reporte.filas().stream()
                .filter(f -> f.fechaApertura().equals(fecha))
                .findFirst()
                .orElseThrow();
    }

    private void assertValores(ValoresMedioPagoCierreDTO valores, String esperado,
                               String declarado, String diferencia, String ingresos,
                               String egresos, String faltante, String sobrante) {
        assertThat(valores.esperado()).isEqualByComparingTo(esperado);
        assertThat(valores.declarado()).isEqualByComparingTo(declarado);
        assertThat(valores.diferencia()).isEqualByComparingTo(diferencia);
        assertThat(valores.ingresos()).isEqualByComparingTo(ingresos);
        assertThat(valores.egresos()).isEqualByComparingTo(egresos);
        assertThat(valores.faltante()).isEqualByComparingTo(faltante);
        assertThat(valores.sobrante()).isEqualByComparingTo(sobrante);
    }

    @Test
    @Transactional
    @DisplayName("Rango por apertura: solo cerradas y pendientes, con todos sus valores")
    void rangoIncluyeCerradasYPendientes() {
        cargarDatos();

        ReporteCierresCajaDTO reporte = reporte();

        assertThat(reporte.filas()).hasSize(2);
        assertThat(reporte.totales().cantidadSesiones()).isEqualTo(2);
        assertThat(reporte.medios()).containsExactly("EFECTIVO", "TRANSFERENCIA");

        CierreCajaFilaDTO primera = fila(reporte, LocalDate.of(2026, 10, 1));
        assertThat(primera.estado()).isEqualTo(EstadoSesionCaja.CERRADA);
        assertThat(primera.pendiente()).isFalse();
        assertThat(primera.cerro()).isEqualTo("Luis Cierra");
        assertThat(primera.montoInicial()).isEqualByComparingTo("100.00");
        assertValores(primera.valoresPorMedio().get("EFECTIVO"),
                "1000.00", "990.00", "-10.00", "1000.00", "50.00", "10.00", "0.00");
        assertValores(primera.valoresPorMedio().get("TRANSFERENCIA"),
                "500.00", "500.00", "0.00", "500.00", "5.00", "0.00", "0.00");

        CierreCajaFilaDTO pendiente = fila(reporte, LocalDate.of(2026, 10, 2));
        assertThat(pendiente.estado()).isEqualTo(EstadoSesionCaja.PENDIENTE_REVISION);
        assertThat(pendiente.pendiente()).isTrue();
        assertThat(pendiente.cerro()).isEqualTo("Viejo Cierre");
        assertValores(pendiente.valoresPorMedio().get("EFECTIVO"),
                "200.00", "210.00", "10.00", "200.00", "0.00", "0.00", "10.00");
    }

    @Test
    @Transactional
    @DisplayName("Los totales suman faltantes y sobrantes por separado")
    void totalesPorMedio() {
        cargarDatos();

        ReporteCierresCajaDTO reporte = reporte();

        assertValores(reporte.totales().porMedio().get("EFECTIVO"),
                "1200.00", "1200.00", "0.00", "1200.00", "50.00", "10.00", "10.00");
        assertValores(reporte.totales().porMedio().get("TRANSFERENCIA"),
                "500.00", "500.00", "0.00", "500.00", "5.00", "0.00", "0.00");
    }

    @Test
    @Transactional
    @DisplayName("El filtro por usuario que cerró incluye inactivos")
    void filtroPorUsuarioCierre_incluyeInactivos() {
        Usuarios usuarios = cargarDatos();

        ReporteCierresCajaDTO reporte = service.cierresCaja(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                usuarios.inactivo(), "TODOS", "TODOS");

        assertThat(reporte.id_usuario_cierre()).isEqualTo(usuarios.inactivo());
        assertThat(reporte.filas()).hasSize(1);
        assertThat(fila(reporte, LocalDate.of(2026, 10, 2)).cerro()).isEqualTo("Viejo Cierre");
        assertThat(reporte.totales().cantidadSesiones()).isEqualTo(1);
    }

    @Test
    @Transactional
    @DisplayName("El filtro por estado acota las filas")
    void filtroPorEstado() {
        cargarDatos();

        ReporteCierresCajaDTO cerradas = service.cierresCaja(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                null, "CERRADA", "TODOS");
        assertThat(cerradas.estado()).isEqualTo("CERRADA");
        assertThat(cerradas.filas()).hasSize(1);
        assertThat(cerradas.filas().get(0).estado()).isEqualTo(EstadoSesionCaja.CERRADA);

        ReporteCierresCajaDTO pendientes = service.cierresCaja(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                null, "PENDIENTE_REVISION", "TODOS");
        assertThat(pendientes.filas()).hasSize(1);
        assertThat(pendientes.filas().get(0).pendiente()).isTrue();
    }

    @Test
    @Transactional
    @DisplayName("El medio de pago acota columnas y totales, sin quitar sesiones")
    void filtroPorMedio_acotaColumnasYTotales() {
        cargarDatos();

        ReporteCierresCajaDTO reporte = service.cierresCaja(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                null, "TODOS", "EFECTIVO");

        assertThat(reporte.medio_pago()).isEqualTo("EFECTIVO");
        assertThat(reporte.medios()).containsExactly("EFECTIVO");
        assertThat(reporte.filas()).hasSize(2);
        assertThat(reporte.filas()).allSatisfy(
                f -> assertThat(f.valoresPorMedio()).containsOnlyKeys("EFECTIVO"));
        assertThat(reporte.totales().porMedio()).containsOnlyKeys("EFECTIVO");
        assertValores(reporte.totales().porMedio().get("EFECTIVO"),
                "1200.00", "1200.00", "0.00", "1200.00", "50.00", "10.00", "10.00");
    }

    @Test
    @Transactional
    @DisplayName("Un rango sin cierres devuelve tabla vacía y totales en cero")
    void sinCierresEnElRango() {
        cargarDatos();

        ReporteCierresCajaDTO reporte = service.cierresCaja(
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 3), null, "TODOS", "TODOS");

        assertThat(reporte.filas()).isEmpty();
        assertThat(reporte.totales().cantidadSesiones()).isZero();
        assertValores(reporte.totales().porMedio().get("EFECTIVO"),
                "0.00", "0.00", "0.00", "0.00", "0.00", "0.00", "0.00");
    }

    @Test
    @DisplayName("Filtros inválidos: 400 de negocio")
    void filtrosInvalidos() {
        assertThatThrownBy(() -> service.cierresCaja(null, LocalDate.of(2026, 10, 1),
                null, "TODOS", "TODOS"))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El rango de fechas es obligatorio");
        assertThatThrownBy(() -> service.cierresCaja(LocalDate.of(2026, 10, 2),
                LocalDate.of(2026, 10, 1), null, "TODOS", "TODOS"))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("La fecha 'desde' no puede ser posterior a 'hasta'");
        assertThatThrownBy(() -> service.cierresCaja(LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 2), null, "ABIERTA", "TODOS"))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El reporte no incluye sesiones abiertas");
        assertThatThrownBy(() -> service.cierresCaja(LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 2), null, "TODOS", "CHEQUE"))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El parámetro 'medio_pago' tiene un valor inválido");
    }
}

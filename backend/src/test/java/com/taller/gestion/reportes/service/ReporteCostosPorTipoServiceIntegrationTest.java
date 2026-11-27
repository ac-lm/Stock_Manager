package com.taller.gestion.reportes.service;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.CostoPorTipoFilaDTO;
import com.taller.gestion.reportes.dto.ReporteCostosPorTipoDTO;
import com.taller.gestion.reportes.dto.TipoCostoReporteDTO;
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
import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
class ReporteCostosPorTipoServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ReporteCostosPorTipoService service;

    private void guardarUsuario() {
        jdbcTemplate.update(
                "INSERT INTO Usuario (id_usuario, nombre, email, password, rol, activo) "
                        + "VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (id_usuario) DO NOTHING",
                601L, "Ana Costos", "anaCostos@taller.local", "hash", "ADMINISTRADOR", true);
    }

    private void guardarTipo(long id, String nombre, String normalizado, boolean activo,
                             boolean reservado) {
        jdbcTemplate.update(
                "INSERT INTO tipo_costo (id_tipo_costo, nombre, nombre_normalizado, activo, "
                        + "reservado) VALUES (?, ?, ?, ?, ?) ON CONFLICT (id_tipo_costo) DO NOTHING",
                id, nombre, normalizado, activo, reservado);
    }

    private void guardarCosto(long id, long idTipo, String monto, int mes, int anio,
                              LocalDate fechaPago, boolean anulado) {
        jdbcTemplate.update(
                "INSERT INTO Costo (id_costo, id_tipo_costo, descripcion, monto, fecha_pago, "
                        + "periodo_mes, periodo_anio, id_usuario, anulado) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, idTipo, "Costo " + id, new BigDecimal(monto), java.sql.Date.valueOf(fechaPago),
                mes, anio, 601L, anulado);
    }

    private ReporteCostosPorTipoDTO reporte(YearMonth desde, YearMonth hasta, Long idTipo) {
        return service.costosPorTipo(desde, hasta, idTipo);
    }

    private CostoPorTipoFilaDTO fila(ReporteCostosPorTipoDTO reporte, String periodo) {
        return reporte.filas().stream()
                .filter(fila -> fila.periodo().equals(periodo))
                .findFirst()
                .orElseThrow();
    }

    private BigDecimal valor(CostoPorTipoFilaDTO fila, long idTipo) {
        return fila.valoresPorTipo().get(String.valueOf(idTipo));
    }

    private BigDecimal totalTipo(ReporteCostosPorTipoDTO reporte, long idTipo) {
        return reporte.tipos().stream()
                .filter(tipo -> tipo.id() == idTipo)
                .map(TipoCostoReporteDTO::total)
                .findFirst()
                .orElseThrow();
    }

    @Test
    @Transactional
    @DisplayName("Agrupa por período y tipo, completa meses vacíos con 0 y excluye anulados")
    void agrupaPorPeriodoYCompletaMeses() {
        guardarUsuario();
        guardarTipo(601L, "Alquiler", "alquiler", true, false);
        guardarTipo(602L, "Luz", "luz", true, false);
        // La fecha de pago está fuera del período: el reporte debe usar periodo_mes/periodo_anio.
        guardarCosto(601L, 601L, "100.00", 10, 2026, LocalDate.of(2026, 12, 15), false);
        guardarCosto(602L, 601L, "200.00", 11, 2026, LocalDate.of(2026, 11, 20), false);
        guardarCosto(603L, 602L, "50.00", 11, 2026, LocalDate.of(2026, 11, 2), false);
        guardarCosto(604L, 602L, "999.00", 12, 2026, LocalDate.of(2026, 12, 2), true);

        ReporteCostosPorTipoDTO reporte = reporte(
                YearMonth.of(2026, 10), YearMonth.of(2026, 12), null);

        assertThat(reporte.tipos()).extracting(TipoCostoReporteDTO::nombre)
                .containsExactly("Alquiler", "Insumos", "Luz");
        assertThat(reporte.filas()).extracting(CostoPorTipoFilaDTO::periodo)
                .containsExactly("2026-10", "2026-11", "2026-12");

        CostoPorTipoFilaDTO octubre = fila(reporte, "2026-10");
        assertThat(valor(octubre, 601L)).isEqualByComparingTo("100.00");
        assertThat(valor(octubre, 602L)).isEqualByComparingTo("0.00");
        assertThat(octubre.total()).isEqualByComparingTo("100.00");

        CostoPorTipoFilaDTO noviembre = fila(reporte, "2026-11");
        assertThat(valor(noviembre, 601L)).isEqualByComparingTo("200.00");
        assertThat(valor(noviembre, 602L)).isEqualByComparingTo("50.00");
        assertThat(noviembre.total()).isEqualByComparingTo("250.00");

        CostoPorTipoFilaDTO diciembre = fila(reporte, "2026-12");
        assertThat(diciembre.total()).isEqualByComparingTo("0.00");

        assertThat(totalTipo(reporte, 601L)).isEqualByComparingTo("300.00");
        assertThat(totalTipo(reporte, 602L)).isEqualByComparingTo("50.00");
        assertThat(totalTipo(reporte, 1L)).isEqualByComparingTo("0.00");
        assertThat(reporte.totales().totalGeneral()).isEqualByComparingTo("350.00");
        assertThat(reporte.totales().registros()).isEqualTo(3);
        assertThat(reporte.totales().meses()).isEqualTo(3);
        assertThat(reporte.avisoInsumos()).contains("CST-06");
    }

    @Test
    @Transactional
    @DisplayName("El filtro por tipo acota columnas; incluye inactivos con costos y tipos sin costo en 0")
    void filtroPorTipo() {
        guardarUsuario();
        guardarTipo(601L, "Alquiler", "alquiler", true, false);
        guardarTipo(602L, "Luz", "luz", true, false);
        guardarTipo(603L, "Servicio viejo", "servicio viejo", false, false);
        guardarCosto(611L, 601L, "100.00", 10, 2026, LocalDate.of(2026, 10, 3), false);
        guardarCosto(612L, 603L, "75.00", 10, 2026, LocalDate.of(2026, 10, 4), false);

        ReporteCostosPorTipoDTO todos = reporte(
                YearMonth.of(2026, 10), YearMonth.of(2026, 10), null);
        assertThat(todos.tipos()).extracting(TipoCostoReporteDTO::nombre)
                .containsExactly("Alquiler", "Insumos", "Servicio viejo");
        assertThat(todos.tiposDisponibles()).extracting("nombre")
                .contains("Alquiler", "Insumos", "Luz", "Servicio viejo");

        ReporteCostosPorTipoDTO viejo = reporte(
                YearMonth.of(2026, 10), YearMonth.of(2026, 10), 603L);
        assertThat(viejo.tipos()).extracting(TipoCostoReporteDTO::nombre)
                .containsExactly("Servicio viejo");
        assertThat(viejo.totales().totalGeneral()).isEqualByComparingTo("75.00");
        assertThat(viejo.totales().registros()).isEqualTo(1);

        ReporteCostosPorTipoDTO sinCostos = reporte(
                YearMonth.of(2026, 10), YearMonth.of(2026, 10), 602L);
        assertThat(sinCostos.tipos()).extracting(TipoCostoReporteDTO::nombre)
                .containsExactly("Luz");
        assertThat(sinCostos.totales().totalGeneral()).isEqualByComparingTo("0.00");
        assertThat(sinCostos.filas().get(0).total()).isEqualByComparingTo("0.00");

        assertThatThrownBy(() -> reporte(
                YearMonth.of(2026, 10), YearMonth.of(2026, 10), 999L))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El tipo de costo no existe");
    }

    @Test
    @DisplayName("Rango obligatorio, invertido, mayor a 36 meses o fuera de 2000-2100: 400")
    void parametrosInvalidos() {
        assertThatThrownBy(() -> reporte(null, YearMonth.of(2026, 10), null))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El rango de períodos es obligatorio");
        assertThatThrownBy(() -> reporte(
                YearMonth.of(2026, 11), YearMonth.of(2026, 10), null))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El período 'desde' no puede ser posterior a 'hasta'");
        assertThatThrownBy(() -> reporte(
                YearMonth.of(2023, 1), YearMonth.of(2026, 1), null))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El rango no puede superar los 36 meses");
        assertThatThrownBy(() -> reporte(
                YearMonth.of(1999, 12), YearMonth.of(1999, 12), null))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("Los períodos deben estar entre 2000-01 y 2100-12");
    }
}

package com.taller.gestion.reportes.service;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.ReporteResultadoPeriodoDTO;
import com.taller.gestion.reportes.dto.ReporteVentasPorPeriodoDTO;
import com.taller.gestion.reportes.dto.ResultadoPeriodoFilaDTO;
import com.taller.gestion.reportes.dto.ReporteCostosPorTipoDTO;
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
class ReporteResultadoPeriodoServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ReporteResultadoPeriodoService service;

    @Autowired
    private ReporteVentasService reporteVentasService;

    @Autowired
    private ReporteCostosPorTipoService reporteCostosPorTipoService;

    private void guardarUsuario() {
        jdbcTemplate.update(
                "INSERT INTO Usuario (id_usuario, nombre, email, password, rol, activo) "
                        + "VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (id_usuario) DO NOTHING",
                5L, "Ana Resultado", "anaResultado@taller.local", "hash", "ADMINISTRADOR", true);
    }

    private void guardarTipo(long id, String nombre, String normalizado) {
        jdbcTemplate.update(
                "INSERT INTO tipo_costo (id_tipo_costo, nombre, nombre_normalizado, activo, "
                        + "reservado) VALUES (?, ?, ?, ?, ?) ON CONFLICT (id_tipo_costo) DO NOTHING",
                id, nombre, normalizado, true, false);
    }

    private void guardarCosto(long id, long idTipo, String monto, int mes, int anio,
                              LocalDate fechaPago, boolean anulado) {
        jdbcTemplate.update(
                "INSERT INTO Costo (id_costo, id_tipo_costo, descripcion, monto, fecha_pago, "
                        + "periodo_mes, periodo_anio, id_usuario, anulado) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, idTipo, "Costo " + id, new BigDecimal(monto), Date.valueOf(fechaPago),
                mes, anio, 5L, anulado);
    }

    private void guardarVenta(LocalDate fecha, String estado, String total) {
        jdbcTemplate.update(
                "INSERT INTO Venta (id_cliente, id_vendedor, fecha, hora, total, estado) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                2L, 5L, Date.valueOf(fecha), Time.valueOf(LocalTime.of(10, 30)),
                new BigDecimal(total), estado);
    }

    private ResultadoPeriodoFilaDTO fila(ReporteResultadoPeriodoDTO reporte, String periodo) {
        return reporte.filas().stream()
                .filter(fila -> fila.periodo().equals(periodo))
                .findFirst()
                .orElseThrow();
    }

    @Test
    @Transactional
    @DisplayName("Cruza ventas por fecha de venta con costos por período, completa meses y excluye anulados")
    void resultadoPorMes() {
        guardarUsuario();
        guardarTipo(701L, "Alquiler", "alquiler");
        guardarVenta(LocalDate.of(2026, 10, 1), "CONFIRMADA", "500.00");
        guardarVenta(LocalDate.of(2026, 10, 15), "CONFIRMADA", "250.00");
        guardarVenta(LocalDate.of(2026, 10, 20), "ANULADA", "999.00");
        guardarVenta(LocalDate.of(2026, 11, 5), "CONFIRMADA", "100.00");
        guardarVenta(LocalDate.of(2026, 12, 1), "CONFIRMADA", "40.00");
        // Costo de octubre pagado en diciembre: cuenta en octubre, no en diciembre.
        guardarCosto(701L, 701L, "200.00", 10, 2026, LocalDate.of(2026, 12, 15), false);
        guardarCosto(702L, 701L, "50.00", 11, 2026, LocalDate.of(2026, 11, 1), false);
        // Un anulado de noviembre y un costo de diciembre sin ventas ese mes.
        guardarCosto(703L, 701L, "999.00", 11, 2026, LocalDate.of(2026, 11, 2), true);

        ReporteResultadoPeriodoDTO reporte = service.resultadoPeriodo(
                YearMonth.of(2026, 10), YearMonth.of(2026, 12));

        assertThat(reporte.filas()).extracting(ResultadoPeriodoFilaDTO::periodo)
                .containsExactly("2026-10", "2026-11", "2026-12");
        assertThat(reporte.filas()).extracting(ResultadoPeriodoFilaDTO::etiqueta)
                .containsExactly("oct 2026", "nov 2026", "dic 2026");

        ResultadoPeriodoFilaDTO octubre = fila(reporte, "2026-10");
        assertThat(octubre.totalVendido()).isEqualByComparingTo("750.00");
        assertThat(octubre.totalCostos()).isEqualByComparingTo("200.00");
        assertThat(octubre.resultado()).isEqualByComparingTo("550.00");

        ResultadoPeriodoFilaDTO noviembre = fila(reporte, "2026-11");
        assertThat(noviembre.totalVendido()).isEqualByComparingTo("100.00");
        assertThat(noviembre.totalCostos()).isEqualByComparingTo("50.00");
        assertThat(noviembre.resultado()).isEqualByComparingTo("50.00");

        ResultadoPeriodoFilaDTO diciembre = fila(reporte, "2026-12");
        assertThat(diciembre.totalVendido()).isEqualByComparingTo("40.00");
        assertThat(diciembre.totalCostos()).isEqualByComparingTo("0.00");
        assertThat(diciembre.resultado()).isEqualByComparingTo("40.00");

        assertThat(reporte.totales().totalVendido()).isEqualByComparingTo("890.00");
        assertThat(reporte.totales().totalCostos()).isEqualByComparingTo("250.00");
        assertThat(reporte.totales().resultado()).isEqualByComparingTo("640.00");
        assertThat(reporte.totales().meses()).isEqualTo(3);
        assertThat(reporte.aclaracion()).contains("no un balance contable");
        assertThat(reporte.avisoInsumos()).contains("CST-06");
    }

    @Test
    @Transactional
    @DisplayName("El resultado puede ser negativo y no se recorta a 0")
    void resultadoNegativo() {
        guardarUsuario();
        guardarTipo(711L, "Alquiler", "alquiler");
        guardarVenta(LocalDate.of(2026, 10, 3), "CONFIRMADA", "100.00");
        guardarCosto(711L, 711L, "350.00", 10, 2026, LocalDate.of(2026, 10, 4), false);

        ReporteResultadoPeriodoDTO reporte = service.resultadoPeriodo(
                YearMonth.of(2026, 10), YearMonth.of(2026, 10));

        assertThat(reporte.filas().get(0).resultado()).isEqualByComparingTo("-250.00");
        assertThat(reporte.totales().resultado()).isEqualByComparingTo("-250.00");
    }

    @Test
    @Transactional
    @DisplayName("Los totales coinciden exactamente con REP-01 y REP-10 sobre el mismo rango")
    void coincideConLosReportesQueReutiliza() {
        guardarUsuario();
        guardarTipo(721L, "Luz", "luz");
        guardarVenta(LocalDate.of(2026, 10, 2), "CONFIRMADA", "120.00");
        guardarVenta(LocalDate.of(2026, 11, 2), "CONFIRMADA", "80.00");
        guardarVenta(LocalDate.of(2026, 11, 3), "ANULADA", "999.00");
        guardarCosto(721L, 721L, "45.00", 10, 2026, LocalDate.of(2026, 10, 2), false);
        guardarCosto(722L, 721L, "70.00", 11, 2026, LocalDate.of(2026, 11, 2), false);
        guardarCosto(723L, 721L, "999.00", 11, 2026, LocalDate.of(2026, 11, 2), true);

        YearMonth desde = YearMonth.of(2026, 10);
        YearMonth hasta = YearMonth.of(2026, 11);
        ReporteVentasPorPeriodoDTO ventas = reporteVentasService.ventasPorPeriodo(
                desde.atDay(1), hasta.atEndOfMonth(), "MES");
        ReporteCostosPorTipoDTO costos = reporteCostosPorTipoService.costosPorTipo(
                desde, hasta, null);
        ReporteResultadoPeriodoDTO reporte = service.resultadoPeriodo(desde, hasta);

        assertThat(reporte.totales().totalVendido())
                .isEqualByComparingTo(ventas.totales().totalVendido());
        assertThat(reporte.totales().totalCostos())
                .isEqualByComparingTo(costos.totales().totalGeneral());
        for (int i = 0; i < ventas.filas().size(); i++) {
            assertThat(reporte.filas().get(i).periodo()).isEqualTo(ventas.filas().get(i).periodo());
            assertThat(reporte.filas().get(i).totalVendido())
                    .isEqualByComparingTo(ventas.filas().get(i).totalVendido());
            assertThat(reporte.filas().get(i).totalCostos())
                    .isEqualByComparingTo(costos.filas().get(i).total());
        }
    }

    @Test
    @DisplayName("Rango obligatorio, invertido, mayor a 36 meses o fuera de 2000-2100")
    void parametrosInvalidos() {
        assertThatThrownBy(() -> service.resultadoPeriodo(null, YearMonth.of(2026, 10)))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El rango de períodos es obligatorio");
        assertThatThrownBy(() -> service.resultadoPeriodo(
                YearMonth.of(2026, 11), YearMonth.of(2026, 10)))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El período 'desde' no puede ser posterior a 'hasta'");
        assertThatThrownBy(() -> service.resultadoPeriodo(
                YearMonth.of(2023, 1), YearMonth.of(2026, 1)))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El rango no puede superar los 36 meses");
        assertThatThrownBy(() -> service.resultadoPeriodo(
                YearMonth.of(1999, 12), YearMonth.of(1999, 12)))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("Los períodos deben estar entre 2000-01 y 2100-12");
    }
}

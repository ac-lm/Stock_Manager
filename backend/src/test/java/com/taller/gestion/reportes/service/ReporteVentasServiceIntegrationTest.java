package com.taller.gestion.reportes.service;

import com.taller.gestion.reportes.dto.AgrupacionPeriodo;
import com.taller.gestion.reportes.dto.ReporteVentasPorPeriodoDTO;
import com.taller.gestion.reportes.dto.VentasPorPeriodoFilaDTO;
import com.taller.gestion.venta.entity.EstadoVenta;
import com.taller.gestion.venta.entity.Venta;
import com.taller.gestion.venta.repository.VentaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
class ReporteVentasServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private ReporteVentasService reporteVentasService;

    private Venta guardarVenta(LocalDate fecha, EstadoVenta estado, String total) {
        Venta venta = new Venta();
        venta.setId_cliente(2L);
        venta.setId_vendedor(5L);
        venta.setFecha(fecha);
        venta.setHora(LocalTime.of(10, 30));
        venta.setTotal(new BigDecimal(total));
        venta.setEstado(estado);
        return ventaRepository.saveAndFlush(venta);
    }

    private void cargarDatos() {
        guardarVenta(LocalDate.of(2026, 10, 1), EstadoVenta.CONFIRMADA, "100.00");
        guardarVenta(LocalDate.of(2026, 10, 1), EstadoVenta.CONFIRMADA, "50.00");
        guardarVenta(LocalDate.of(2026, 10, 2), EstadoVenta.ANULADA, "999.00");
        guardarVenta(LocalDate.of(2026, 10, 3), EstadoVenta.CONFIRMADA, "200.00");

        // Confirmada en octubre pero anulada después: tampoco debe contar.
        Venta anuladaDespues = guardarVenta(
                LocalDate.of(2026, 10, 4), EstadoVenta.CONFIRMADA, "400.00");
        anuladaDespues.setEstado(EstadoVenta.ANULADA);
        ventaRepository.saveAndFlush(anuladaDespues);

        guardarVenta(LocalDate.of(2026, 10, 5), EstadoVenta.CONFIRMADA, "300.00");
        guardarVenta(LocalDate.of(2026, 10, 31), EstadoVenta.CONFIRMADA, "150.00");
        // Fuera del rango.
        guardarVenta(LocalDate.of(2026, 11, 1), EstadoVenta.CONFIRMADA, "500.00");
    }

    private VentasPorPeriodoFilaDTO fila(ReporteVentasPorPeriodoDTO reporte, String periodo) {
        return reporte.filas().stream()
                .filter(f -> f.periodo().equals(periodo))
                .findFirst()
                .orElseThrow();
    }

    @Test
    @Transactional
    @DisplayName("Por día: suma Venta.total de confirmadas, excluye anuladas y completa ceros")
    void ventasPorDia() {
        cargarDatos();

        ReporteVentasPorPeriodoDTO reporte = reporteVentasService.ventasPorPeriodo(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), "dia");

        assertThat(reporte.agrupacion()).isEqualTo(AgrupacionPeriodo.DIA);
        assertThat(reporte.filas()).hasSize(31); // octubre completo, incluidos los días sin ventas

        VentasPorPeriodoFilaDTO primero = fila(reporte, "2026-10-01");
        assertThat(primero.etiqueta()).isEqualTo("2026-10-01");
        assertThat(primero.totalVendido()).isEqualByComparingTo("150.00");
        assertThat(primero.cantidadVentas()).isEqualTo(2);
        assertThat(primero.ticketPromedio()).isEqualByComparingTo("75.00");

        assertThat(fila(reporte, "2026-10-02").totalVendido()).isEqualByComparingTo("0.00");
        assertThat(fila(reporte, "2026-10-02").cantidadVentas()).isZero();
        assertThat(fila(reporte, "2026-10-04").totalVendido()).isEqualByComparingTo("0.00");
        assertThat(fila(reporte, "2026-10-31").totalVendido()).isEqualByComparingTo("150.00");

        assertThat(reporte.filas())
                .noneMatch(f -> f.periodo().equals("2026-11-01"));
        assertThat(reporte.totales().totalVendido()).isEqualByComparingTo("800.00");
        assertThat(reporte.totales().cantidadVentas()).isEqualTo(5);
        assertThat(reporte.totales().ticketPromedio()).isEqualByComparingTo("160.00");
    }

    @Test
    @Transactional
    @DisplayName("Por semana: etiqueta el lunes y agrupa los días de esa semana")
    void ventasPorSemana() {
        cargarDatos();

        ReporteVentasPorPeriodoDTO reporte = reporteVentasService.ventasPorPeriodo(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), "semana");

        assertThat(reporte.agrupacion()).isEqualTo(AgrupacionPeriodo.SEMANA);
        String lunesPrimeraSemana = LocalDate.of(2026, 10, 1)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString();
        assertThat(lunesPrimeraSemana).isEqualTo("2026-09-28");

        assertThat(reporte.filas()).hasSize(5);
        assertThat(fila(reporte, lunesPrimeraSemana).etiqueta())
                .isEqualTo("Semana del " + lunesPrimeraSemana);
        assertThat(fila(reporte, lunesPrimeraSemana).totalVendido())
                .isEqualByComparingTo("350.00"); // 01 (150) y 03 (200); la anulada del 02 no cuenta
        assertThat(fila(reporte, "2026-10-05").totalVendido()).isEqualByComparingTo("300.00");
        assertThat(fila(reporte, "2026-10-12").totalVendido()).isEqualByComparingTo("0.00");
        assertThat(fila(reporte, "2026-10-26").totalVendido()).isEqualByComparingTo("150.00");
        assertThat(reporte.totales().totalVendido()).isEqualByComparingTo("800.00");
    }

    @Test
    @Transactional
    @DisplayName("Por mes: una fila 2026-10 con el total del rango")
    void ventasPorMes() {
        cargarDatos();

        ReporteVentasPorPeriodoDTO reporte = reporteVentasService.ventasPorPeriodo(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), "mes");

        assertThat(reporte.agrupacion()).isEqualTo(AgrupacionPeriodo.MES);
        assertThat(reporte.filas()).hasSize(1);
        VentasPorPeriodoFilaDTO octubre = reporte.filas().get(0);
        assertThat(octubre.periodo()).isEqualTo("2026-10");
        assertThat(octubre.etiqueta()).isEqualTo("oct 2026");
        assertThat(octubre.totalVendido()).isEqualByComparingTo("800.00");
        assertThat(octubre.cantidadVentas()).isEqualTo(5);
        assertThat(octubre.ticketPromedio()).isEqualByComparingTo("160.00");
    }

    @Test
    @Transactional
    @DisplayName("Sin ventas: todas las filas y los totales valen cero")
    void rangoSinVentas() {
        ReporteVentasPorPeriodoDTO reporte = reporteVentasService.ventasPorPeriodo(
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 3), "dia");

        assertThat(reporte.filas()).hasSize(3);
        assertThat(reporte.filas()).allSatisfy(fila -> {
            assertThat(fila.totalVendido()).isEqualByComparingTo("0.00");
            assertThat(fila.cantidadVentas()).isZero();
            assertThat(fila.ticketPromedio()).isEqualByComparingTo("0.00");
        });
        assertThat(reporte.totales().totalVendido()).isEqualByComparingTo("0.00");
        assertThat(reporte.totales().ticketPromedio()).isEqualByComparingTo("0.00");
    }

    @Test
    @Transactional
    @DisplayName("El último día del rango se incluye completo")
    void limiteInclusivo() {
        guardarVenta(LocalDate.of(2026, 10, 31), EstadoVenta.CONFIRMADA, "12.34");

        ReporteVentasPorPeriodoDTO reporte = reporteVentasService.ventasPorPeriodo(
                LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 31), "dia");

        assertThat(reporte.filas()).hasSize(1);
        assertThat(reporte.filas().get(0).totalVendido()).isEqualByComparingTo("12.34");
    }
}

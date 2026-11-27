package com.taller.gestion.reportes.service;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.reportes.dto.ConteoIncidenciaPorTipoDTO;
import com.taller.gestion.reportes.dto.IncidenciaDescartesFilaDTO;
import com.taller.gestion.reportes.dto.ReporteIncidenciasDescartesDTO;
import com.taller.gestion.reportes.dto.TipoIncidenciaOpcionDTO;
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
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
class ReporteIncidenciasDescartesServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ReporteIncidenciasDescartesService service;

    private void guardarUsuario(long id) {
        jdbcTemplate.update(
                "INSERT INTO Usuario (id_usuario, nombre, email, password, rol, activo) "
                        + "VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (id_usuario) DO NOTHING",
                id, "Ana", "ana" + id + "@stock.local", "x", "ADMINISTRADOR", true);
    }

    private void guardarTipo(long id, String nombre, boolean activo) {
        jdbcTemplate.update(
                "INSERT INTO Tipo_Incidencia (id_tipo_incidencia, nombre, activo) "
                        + "VALUES (?, ?, ?)",
                id, nombre, activo);
    }

    private void guardarProducto(long id, String nombre) {
        jdbcTemplate.update(
                "INSERT INTO Producto (id_producto, nombre, precio, stock_actual, categoria) "
                        + "VALUES (?, ?, ?, ?, ?)",
                id, nombre, new BigDecimal("100.00"), BigDecimal.ZERO, "Prueba");
    }

    private void guardarInsumo(long id, String nombre, String unidad) {
        jdbcTemplate.update(
                "INSERT INTO Insumo (id_insumo, nombre, stock_actual, unidad_medida, "
                        + "stock_minimo, costo) VALUES (?, ?, ?, ?, ?, ?)",
                id, nombre, BigDecimal.ZERO, unidad, 1, BigDecimal.ZERO);
    }

    private void guardarIncidencia(long id, long idTipo, Long idProducto, Long idInsumo,
                                   String estado, String cantidad, LocalDateTime fecha) {
        jdbcTemplate.update(
                "INSERT INTO Incidencia (id_incidencia, id_tipo_incidencia, id_producto, "
                        + "id_insumo, descripcion, cantidad_afectada, estado, fecha, id_usuario) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, idTipo, idProducto, idInsumo, "Prueba", new BigDecimal(cantidad), estado,
                Timestamp.valueOf(fecha), 77L);
    }

    private void guardarMovimiento(long id, Long idProducto, Long idInsumo, String tipo,
                                   String motivo, String cantidad, LocalDateTime fechaHora,
                                   Long idVenta) {
        jdbcTemplate.update(
                "INSERT INTO Movimiento_Stock (id_movimiento_stock, id_producto, id_insumo, "
                        + "tipo, cantidad, motivo, id_usuario, fecha_hora, id_venta) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, idProducto, idInsumo, tipo, new BigDecimal(cantidad), motivo, 1L,
                Timestamp.valueOf(fechaHora), idVenta);
    }

    private void guardarVenta(long id) {
        jdbcTemplate.update(
                "INSERT INTO Venta (id_venta, id_cliente, id_vendedor, fecha, hora, total, estado) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                id, 900L, 1L, Date.valueOf(LocalDate.of(2026, 11, 5)),
                Time.valueOf(LocalTime.of(10, 0)), new BigDecimal("10.00"), "CONFIRMADA");
    }

    private ReporteIncidenciasDescartesDTO reporte() {
        return service.incidenciasDescartes(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                null, "TODOS", "TODOS", null);
    }

    private IncidenciaDescartesFilaDTO fila(ReporteIncidenciasDescartesDTO reporte,
                                            String articulo, String estado) {
        return reporte.filas().stream()
                .filter(f -> f.articulo().equals(articulo) && f.estado().equals(estado))
                .findFirst()
                .orElseThrow();
    }

    @Test
    @Transactional
    @DisplayName("Agrupa por tipo, artículo y estado y asigna cada descarte una sola vez")
    void agrupaYCruzaSinDuplicarDescartes() {
        guardarUsuario(1L);
        guardarTipo(101L, "Rotura", true);
        guardarProducto(201L, "Café");
        guardarInsumo(202L, "Harina", "kg");
        guardarIncidencia(1L, 101L, 201L, null, "ABIERTA", "2",
                LocalDateTime.of(2026, 11, 5, 9, 0));
        guardarIncidencia(2L, 101L, 201L, null, "CERRADA", "1.500",
                LocalDateTime.of(2026, 11, 10, 9, 0));
        guardarIncidencia(3L, 101L, null, 202L, "ABIERTA", "3",
                LocalDateTime.of(2026, 11, 11, 9, 0));
        guardarMovimiento(1L, 201L, null, "SALIDA", "MERMA", "1",
                LocalDateTime.of(2026, 11, 6, 10, 0), null);
        guardarMovimiento(2L, 201L, null, "SALIDA", "MERMA", "0.500",
                LocalDateTime.of(2026, 11, 7, 10, 0), null);
        guardarMovimiento(3L, null, 202L, "SALIDA", "MERMA", "2",
                LocalDateTime.of(2026, 11, 12, 10, 0), null);

        ReporteIncidenciasDescartesDTO reporte = reporte();

        assertThat(reporte.filas()).hasSize(3);
        IncidenciaDescartesFilaDTO abierta = fila(reporte, "Café", "ABIERTA");
        IncidenciaDescartesFilaDTO cerrada = fila(reporte, "Café", "CERRADA");
        IncidenciaDescartesFilaDTO harina = fila(reporte, "Harina", "ABIERTA");
        assertThat(abierta.incidencias()).isEqualTo(1);
        assertThat(abierta.cantidadAfectada()).isEqualByComparingTo("2");
        assertThat(abierta.movimientosMerma()).isEqualTo(2);
        assertThat(abierta.cantidadDescartada()).isEqualByComparingTo("1.500");
        assertThat(abierta.posibleTraslape()).isTrue();
        // El descarte del mismo artículo no se reparte entre la fila abierta y la cerrada.
        assertThat(cerrada.incidencias()).isEqualTo(1);
        assertThat(cerrada.cantidadAfectada()).isEqualByComparingTo("1.500");
        assertThat(cerrada.movimientosMerma()).isZero();
        assertThat(cerrada.cantidadDescartada()).isEqualByComparingTo("0");
        assertThat(cerrada.posibleTraslape()).isFalse();
        assertThat(harina.unidad()).isEqualTo("kg");
        assertThat(harina.cantidadAfectada()).isEqualByComparingTo("3");
        assertThat(harina.cantidadDescartada()).isEqualByComparingTo("2");
        assertThat(reporte.totales().incidencias()).isEqualTo(3);
        assertThat(reporte.totales().abiertas()).isEqualTo(2);
        assertThat(reporte.totales().cerradas()).isEqualTo(1);
        assertThat(reporte.totales().movimientosMerma()).isEqualTo(3);
        assertThat(reporte.totales().articulos()).isEqualTo(2);
        assertThat(reporte.incidenciasPorTipo())
                .extracting(ConteoIncidenciaPorTipoDTO::incidencias)
                .containsExactly(3L);
        assertThat(reporte.avisos()).hasSize(6);
        assertThat(reporte.avisos().get(0)).contains("INC-07");
    }

    @Test
    @Transactional
    @DisplayName("El rango incluye ambos días completos y solo cuenta SALIDA/MERMA sin venta")
    void rangoInclusivoYDefinicionDeDescarte() {
        guardarUsuario(1L);
        guardarTipo(101L, "Rotura", true);
        guardarInsumo(202L, "Harina", "kg");
        guardarIncidencia(1L, 101L, null, 202L, "ABIERTA", "1",
                LocalDateTime.of(2026, 11, 1, 0, 0));
        guardarIncidencia(2L, 101L, null, 202L, "ABIERTA", "2",
                LocalDateTime.of(2026, 11, 30, 23, 59, 59));
        guardarIncidencia(3L, 101L, null, 202L, "ABIERTA", "99",
                LocalDateTime.of(2026, 12, 1, 0, 0));
        guardarMovimiento(1L, null, 202L, "SALIDA", "MERMA", "0.500",
                LocalDateTime.of(2026, 11, 1, 0, 0), null);
        guardarMovimiento(2L, null, 202L, "SALIDA", "MERMA", "0.250",
                LocalDateTime.of(2026, 11, 30, 23, 59, 59), null);
        guardarMovimiento(3L, null, 202L, "SALIDA", "MERMA", "99",
                LocalDateTime.of(2026, 12, 1, 0, 0), null);
        guardarMovimiento(4L, null, 202L, "ENTRADA", "MERMA", "1",
                LocalDateTime.of(2026, 11, 10, 10, 0), null);
        guardarVenta(500L);
        guardarMovimiento(5L, null, 202L, "SALIDA", "MERMA", "1",
                LocalDateTime.of(2026, 11, 10, 10, 0), 500L);

        ReporteIncidenciasDescartesDTO reporte = reporte();

        assertThat(reporte.filas()).hasSize(1);
        IncidenciaDescartesFilaDTO harina = reporte.filas().get(0);
        assertThat(harina.incidencias()).isEqualTo(2);
        assertThat(harina.cantidadAfectada()).isEqualByComparingTo("3");
        assertThat(harina.movimientosMerma()).isEqualTo(2);
        assertThat(harina.cantidadDescartada()).isEqualByComparingTo("0.750");
        assertThat(reporte.totales().incidencias()).isEqualTo(2);
        assertThat(reporte.totales().movimientosMerma()).isEqualTo(2);
    }

    @Test
    @Transactional
    @DisplayName("Los descartes sin incidencia se listan aparte y no se duplican entre filas")
    void descarteSinIncidenciaSeMuestraAparte() {
        guardarUsuario(1L);
        guardarTipo(101L, "Rotura", true);
        guardarProducto(201L, "Café");
        guardarInsumo(203L, "Leche", "lt");
        guardarIncidencia(1L, 101L, 201L, null, "ABIERTA", "2",
                LocalDateTime.of(2026, 11, 5, 9, 0));
        guardarIncidencia(2L, 101L, 201L, null, "CERRADA", "1",
                LocalDateTime.of(2026, 11, 6, 9, 0));
        guardarMovimiento(1L, 201L, null, "SALIDA", "MERMA", "1",
                LocalDateTime.of(2026, 11, 7, 10, 0), null);
        guardarMovimiento(2L, null, 203L, "SALIDA", "MERMA", "4",
                LocalDateTime.of(2026, 11, 8, 10, 0), null);

        ReporteIncidenciasDescartesDTO reporte = reporte();

        assertThat(reporte.filas()).extracting(IncidenciaDescartesFilaDTO::tipoIncidencia)
                .containsExactly("Rotura", "Rotura", "Sin incidencia");
        assertThat(fila(reporte, "Leche", "—").incidencias()).isZero();
        assertThat(fila(reporte, "Leche", "—").movimientosMerma()).isEqualTo(1);
        assertThat(fila(reporte, "Leche", "—").cantidadDescartada())
                .isEqualByComparingTo("4");
        assertThat(fila(reporte, "Leche", "—").posibleTraslape()).isFalse();
        assertThat(reporte.totales().articulos()).isEqualTo(2);
        assertThat(reporte.totales().movimientosMerma()).isEqualTo(2);
    }

    @Test
    @Transactional
    @DisplayName("Los filtros acotan la tabla; el selector conserva tipos inactivos con historial")
    void filtrosYSelectorDeTipos() {
        guardarUsuario(1L);
        guardarTipo(101L, "Rotura", true);
        guardarTipo(102L, "Vencimiento", false);
        guardarProducto(201L, "Café");
        guardarInsumo(202L, "Harina", "kg");
        guardarIncidencia(1L, 101L, 201L, null, "ABIERTA", "2",
                LocalDateTime.of(2026, 11, 5, 9, 0));
        guardarIncidencia(2L, 102L, null, 202L, "CERRADA", "1",
                LocalDateTime.of(2026, 11, 6, 9, 0));

        ReporteIncidenciasDescartesDTO todo = reporte();
        assertThat(todo.tiposDisponibles())
                .extracting(TipoIncidenciaOpcionDTO::nombre)
                .containsExactly("Rotura", "Vencimiento");
        assertThat(todo.tiposDisponibles().get(1).activo()).isFalse();

        ReporteIncidenciasDescartesDTO cerradas = service.incidenciasDescartes(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                null, "CERRADA", "TODOS", null);
        assertThat(cerradas.filas()).extracting(IncidenciaDescartesFilaDTO::articulo)
                .containsExactly("Harina");
        assertThat(cerradas.totales().incidencias()).isEqualTo(1);

        ReporteIncidenciasDescartesDTO porArticulo = service.incidenciasDescartes(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                null, "TODOS", "INSUMO", 202L);
        assertThat(porArticulo.filas()).hasSize(1);
        assertThat(porArticulo.idArticulo()).isEqualTo(202L);
        assertThat(porArticulo.tipoArticulo()).isEqualTo("INSUMO");

        ReporteIncidenciasDescartesDTO tipoVacio = service.incidenciasDescartes(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                999L, "TODOS", "TODOS", null);
        assertThat(tipoVacio.filas()).hasSize(1);
        assertThat(tipoVacio.filas().get(0).articulo()).isEqualTo("—");
        assertThat(tipoVacio.filas().get(0).tipoIncidencia()).isEqualTo("Tipo #999");
        assertThat(tipoVacio.totales().incidencias()).isZero();
    }

    @Test
    @Transactional
    @DisplayName("Una incidencia con producto e insumo cuenta dos filas pero una sola en el gráfico")
    void incidenciaConDosArticulos() {
        guardarUsuario(1L);
        guardarTipo(101L, "Rotura", true);
        guardarProducto(201L, "Café");
        guardarInsumo(202L, "Harina", "kg");
        guardarIncidencia(1L, 101L, 201L, 202L, "ABIERTA", "3",
                LocalDateTime.of(2026, 11, 5, 9, 0));
        guardarMovimiento(1L, 201L, null, "SALIDA", "MERMA", "1",
                LocalDateTime.of(2026, 11, 6, 10, 0), null);
        guardarMovimiento(2L, null, 202L, "SALIDA", "MERMA", "2",
                LocalDateTime.of(2026, 11, 6, 10, 0), null);

        ReporteIncidenciasDescartesDTO reporte = reporte();

        assertThat(reporte.filas()).hasSize(2);
        assertThat(reporte.totales().incidencias()).isEqualTo(1);
        assertThat(reporte.incidenciasPorTipo())
                .extracting(ConteoIncidenciaPorTipoDTO::incidencias)
                .containsExactly(1L);
        assertThat(reporte.filas())
                .allSatisfy(fila -> assertThat(fila.posibleTraslape()).isTrue());
    }

    @Test
    @Transactional
    @DisplayName("Filtrar por tipo de artículo sin id muestra solo ese lado de una incidencia mixta")
    void filtroPorTipoDeArticuloSinId() {
        guardarUsuario(1L);
        guardarTipo(101L, "Rotura", true);
        guardarProducto(201L, "Café");
        guardarInsumo(202L, "Harina", "kg");
        guardarIncidencia(1L, 101L, 201L, 202L, "ABIERTA", "3",
                LocalDateTime.of(2026, 11, 5, 9, 0));
        guardarIncidencia(2L, 101L, null, 202L, "ABIERTA", "1",
                LocalDateTime.of(2026, 11, 6, 9, 0));
        guardarMovimiento(1L, 201L, null, "SALIDA", "MERMA", "1",
                LocalDateTime.of(2026, 11, 6, 10, 0), null);
        guardarMovimiento(2L, null, 202L, "SALIDA", "MERMA", "2",
                LocalDateTime.of(2026, 11, 7, 10, 0), null);

        ReporteIncidenciasDescartesDTO productos = service.incidenciasDescartes(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                null, "TODOS", "PRODUCTO", null);
        assertThat(productos.tipoArticulo()).isEqualTo("PRODUCTO");
        assertThat(productos.filas()).extracting(IncidenciaDescartesFilaDTO::articulo)
                .containsExactly("Café");
        assertThat(productos.filas().get(0).cantidadAfectada()).isEqualByComparingTo("3");
        assertThat(productos.filas().get(0).movimientosMerma()).isEqualTo(1);
        assertThat(productos.filas().get(0).cantidadDescartada()).isEqualByComparingTo("1");
        assertThat(productos.totales().incidencias()).isEqualTo(1);
        assertThat(productos.totales().movimientosMerma()).isEqualTo(1);
        assertThat(productos.totales().articulos()).isEqualTo(1);
        assertThat(productos.incidenciasPorTipo())
                .extracting(ConteoIncidenciaPorTipoDTO::incidencias)
                .containsExactly(1L);

        ReporteIncidenciasDescartesDTO insumos = service.incidenciasDescartes(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                null, "TODOS", "INSUMO", null);
        assertThat(insumos.tipoArticulo()).isEqualTo("INSUMO");
        assertThat(insumos.filas()).extracting(IncidenciaDescartesFilaDTO::articulo)
                .containsExactly("Harina");
        assertThat(insumos.filas().get(0).incidencias()).isEqualTo(2);
        assertThat(insumos.filas().get(0).cantidadAfectada()).isEqualByComparingTo("4");
        assertThat(insumos.filas().get(0).movimientosMerma()).isEqualTo(1);
        assertThat(insumos.filas().get(0).cantidadDescartada()).isEqualByComparingTo("2");
        assertThat(insumos.totales().incidencias()).isEqualTo(2);
        assertThat(insumos.totales().movimientosMerma()).isEqualTo(1);
        assertThat(insumos.totales().articulos()).isEqualTo(1);
        assertThat(insumos.incidenciasPorTipo())
                .extracting(ConteoIncidenciaPorTipoDTO::incidencias)
                .containsExactly(2L);
    }

    @Test
    @DisplayName("Rango, estado, tipo de artículo y artículo inválidos se rechazan como 400")
    void parametrosInvalidos() {
        assertThatThrownBy(() -> service.incidenciasDescartes(
                null, LocalDate.of(2026, 11, 30), null, "TODOS", "TODOS", null))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El rango de fechas es obligatorio");
        assertThatThrownBy(() -> service.incidenciasDescartes(
                LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 1),
                null, "TODOS", "TODOS", null))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("La fecha 'desde' no puede ser posterior a 'hasta'");
        assertThatThrownBy(() -> service.incidenciasDescartes(
                LocalDate.of(2020, 1, 1), LocalDate.of(2026, 1, 1),
                null, "TODOS", "TODOS", null))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El rango no puede superar los 5 años");
        assertThatThrownBy(() -> service.incidenciasDescartes(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                null, "PENDIENTE", "TODOS", null))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El parámetro 'estado' tiene un valor inválido");
        assertThatThrownBy(() -> service.incidenciasDescartes(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                null, "TODOS", "BEBIDA", null))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El parámetro 'tipo_articulo' tiene un valor inválido");
        assertThatThrownBy(() -> service.incidenciasDescartes(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                null, "TODOS", "TODOS", 10L))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El artículo requiere indicar si es producto o insumo");
    }
}

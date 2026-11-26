package com.taller.gestion.stock.repository;

import com.taller.gestion.catalogo.entity.Insumo;
import com.taller.gestion.catalogo.entity.Producto;
import com.taller.gestion.catalogo.repository.InsumoRepository;
import com.taller.gestion.catalogo.repository.ProductoRepository;
import com.taller.gestion.stock.entity.MotivoMovimientoStock;
import com.taller.gestion.stock.entity.MovimientoStock;
import com.taller.gestion.stock.entity.TipoMovimientoStock;
import com.taller.gestion.usuario.entity.Rol;
import com.taller.gestion.usuario.entity.Usuario;
import com.taller.gestion.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class MovimientoStockRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private MovimientoStockRepository movimientoStockRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private InsumoRepository insumoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario usuario() {
        Usuario u = new Usuario();
        u.setNombre("Ana");
        u.setEmail("ana@stock.local");
        u.setPassword("x");
        u.setRol(Rol.VENDEDOR);
        u.setActivo(true);
        return usuarioRepository.save(u);
    }

    private Producto producto(String nombre) {
        Producto p = new Producto();
        p.setNombre(nombre);
        p.setPrecio(new BigDecimal("100.00"));
        p.setStockActual(BigDecimal.ZERO);
        p.setCategoria("Prueba");
        return productoRepository.save(p);
    }

    private Insumo insumo(String nombre) {
        Insumo i = new Insumo();
        i.setNombre(nombre);
        i.setStockActual(BigDecimal.ZERO);
        i.setUnidadMedida("kg");
        i.setStockMinimo(1);
        i.setCosto(BigDecimal.ZERO);
        return insumoRepository.save(i);
    }

    private MovimientoStock movimiento(Producto producto, Insumo insumo, Usuario usuario,
                                       TipoMovimientoStock tipo, MotivoMovimientoStock motivo,
                                       String cantidad, LocalDateTime fechaHora) {
        MovimientoStock m = new MovimientoStock();
        m.setProducto(producto);
        m.setInsumo(insumo);
        m.setTipo(tipo);
        m.setMotivo(motivo);
        m.setCantidad(new BigDecimal(cantidad));
        m.setUsuario(usuario);
        m.setFechaHora(fechaHora);
        return movimientoStockRepository.save(m);
    }

    private PageRequest pagina() {
        return PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "fechaHora"));
    }

    private void cargarDatos() {
        Usuario ana = usuario();
        Producto p1 = producto("Producto 1");
        Insumo i1 = insumo("Insumo 1");
        movimiento(p1, null, ana, TipoMovimientoStock.ENTRADA, MotivoMovimientoStock.INICIAL,
                "10.000", LocalDateTime.of(2026, 11, 1, 10, 0));
        movimiento(p1, null, ana, TipoMovimientoStock.SALIDA, MotivoMovimientoStock.AJUSTE,
                "3.000", LocalDateTime.of(2026, 11, 2, 10, 0));
        movimiento(null, i1, ana, TipoMovimientoStock.ENTRADA, MotivoMovimientoStock.COMPRA,
                "5.000", LocalDateTime.of(2026, 11, 3, 10, 0));
    }

    @Test
    @DisplayName("El kardex de un producto devuelve sus movimientos del más nuevo al más viejo")
    void kardexPorProducto_ordenado() {
        Producto p = producto("Producto kardex");
        Usuario ana = usuario();
        movimiento(p, null, ana, TipoMovimientoStock.ENTRADA, MotivoMovimientoStock.INICIAL,
                "1.000", LocalDateTime.of(2026, 11, 1, 9, 0));
        movimiento(p, null, ana, TipoMovimientoStock.SALIDA, MotivoMovimientoStock.AJUSTE,
                "2.000", LocalDateTime.of(2026, 11, 2, 9, 0));

        List<MovimientoStock> resultado = movimientoStockRepository.listarPorProducto(p.getId_producto());

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getTipo()).isEqualTo(TipoMovimientoStock.SALIDA);
        assertThat(resultado.get(1).getTipo()).isEqualTo(TipoMovimientoStock.ENTRADA);
    }

    @Test
    @DisplayName("El historial filtra por tipo y motivo")
    void historial_filtraPorTipoYMotivo() {
        cargarDatos();

        Page<MovimientoStock> porTipo = movimientoStockRepository.listar(
                TipoMovimientoStock.SALIDA, null, null, null, null, null, null, pagina());
        assertThat(porTipo.getTotalElements()).isEqualTo(1);
        assertThat(porTipo.getContent().get(0).getMotivo()).isEqualTo(MotivoMovimientoStock.AJUSTE);

        Page<MovimientoStock> porMotivo = movimientoStockRepository.listar(
                null, MotivoMovimientoStock.INICIAL, null, null, null, null, null, pagina());
        assertThat(porMotivo.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("El historial filtra por producto, insumo y usuario")
    void historial_filtraPorItemYUsuario() {
        cargarDatos();
        Long idProducto = productoRepository.findAll().stream()
                .filter(p -> p.getNombre().equals("Producto 1")).findFirst().orElseThrow().getId_producto();
        Long idInsumo = insumoRepository.findAll().get(0).getId_insumo();
        Long idUsuario = usuarioRepository.findAll().get(0).getId_usuario();

        assertThat(movimientoStockRepository.listar(
                null, null, idProducto, null, null, null, null, pagina())
                .getTotalElements()).isEqualTo(2);
        assertThat(movimientoStockRepository.listar(
                null, null, null, idInsumo, null, null, null, pagina())
                .getTotalElements()).isEqualTo(1);
        assertThat(movimientoStockRepository.listar(
                null, null, null, null, idUsuario, null, null, pagina())
                .getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("El historial filtra por rango de fecha_hora y ordena descendente")
    void historial_filtraPorFechaYOrdena() {
        cargarDatos();

        Page<MovimientoStock> desde = movimientoStockRepository.listar(
                null, null, null, null, null,
                LocalDateTime.of(2026, 11, 2, 0, 0), null, pagina());
        assertThat(desde.getTotalElements()).isEqualTo(2);
        assertThat(desde.getContent().get(0).getFechaHora())
                .isAfter(desde.getContent().get(1).getFechaHora());

        Page<MovimientoStock> hasta = movimientoStockRepository.listar(
                null, null, null, null, null,
                null, LocalDateTime.of(2026, 11, 1, 23, 59), pagina());
        assertThat(hasta.getTotalElements()).isEqualTo(1);
    }
}

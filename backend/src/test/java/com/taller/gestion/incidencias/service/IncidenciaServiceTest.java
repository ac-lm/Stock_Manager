package com.taller.gestion.incidencias.service;

import com.taller.gestion.catalogo.entity.Insumo;
import com.taller.gestion.catalogo.entity.Producto;
import com.taller.gestion.catalogo.service.InsumoService;
import com.taller.gestion.catalogo.service.ProductoService;
import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.common.exception.OperacionNoPermitidaException;
import com.taller.gestion.common.exception.RecursoNoEncontradoException;
import com.taller.gestion.incidencias.dto.IncidenciaCreateDTO;
import com.taller.gestion.incidencias.dto.IncidenciaResolucionDTO;
import com.taller.gestion.incidencias.dto.IncidenciaResponseDTO;
import com.taller.gestion.incidencias.dto.IncidenciaUpdateDTO;
import com.taller.gestion.incidencias.entity.EstadoIncidencia;
import com.taller.gestion.incidencias.entity.Incidencia;
import com.taller.gestion.incidencias.entity.TipoIncidencia;
import com.taller.gestion.incidencias.repository.IncidenciaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidenciaServiceTest {

    @Mock
    private IncidenciaRepository incidenciaRepository;

    @Mock
    private TipoIncidenciaService tipoIncidenciaService;

    @Mock
    private InsumoService insumoService;

    @Mock
    private ProductoService productoService;

    @InjectMocks
    private IncidenciaService incidenciaService;

    @Test
    void crearSinProductoNiInsumoLanzaError() {
        IncidenciaCreateDTO dto = new IncidenciaCreateDTO(
                1L, null, null, new BigDecimal("1.000"), "Rotura", null, null);

        assertThatThrownBy(() -> incidenciaService.crearIncidencia(dto))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("al menos un producto o un insumo");

        verifyNoInteractions(incidenciaRepository, tipoIncidenciaService);
    }

    @Test
    void crearConCantidadCeroLanzaError() {
        IncidenciaCreateDTO dto = new IncidenciaCreateDTO(
                1L, 2L, null, BigDecimal.ZERO, "Rotura", null, null);

        assertThatThrownBy(() -> incidenciaService.crearIncidencia(dto))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("mayor a cero");

        verifyNoInteractions(incidenciaRepository, tipoIncidenciaService);
    }

    @Test
    void crearConTipoInactivoLanzaError() {
        IncidenciaCreateDTO dto = new IncidenciaCreateDTO(
                1L, 2L, null, new BigDecimal("1.000"), "Rotura", null, null);

        when(tipoIncidenciaService.obtenerActivoPorId(1L))
                .thenThrow(new DatosInvalidosException("El tipo de incidencia está inactivo"));

        assertThatThrownBy(() -> incidenciaService.crearIncidencia(dto))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("inactivo");

        verify(incidenciaRepository, never()).save(any());
    }

    @Test
    void crearConProductoEInsumoGuardaAmbosYEstadoAbierto() {
        TipoIncidencia tipo = tipoActivo();
        Insumo insumo = insumo(2L, "Tinta");
        Producto producto = producto(3L, "Banner");

        when(tipoIncidenciaService.obtenerActivoPorId(1L)).thenReturn(tipo);
        when(insumoService.obtenerPorId(2L)).thenReturn(insumo);
        when(productoService.obtenerPorId(3L)).thenReturn(producto);
        when(incidenciaRepository.save(any(Incidencia.class))).thenAnswer(invocation -> {
            Incidencia guardada = invocation.getArgument(0);
            guardada.setId_incidencia(99L);
            return guardada;
        });

        IncidenciaCreateDTO dto = new IncidenciaCreateDTO(
                1L, 2L, 3L, new BigDecimal("1.500"), "Rotura", "Se rompió", 7L);

        IncidenciaResponseDTO response = incidenciaService.crearIncidencia(dto);

        assertThat(response.id_incidencia()).isEqualTo(99L);
        assertThat(response.id_tipo_incidencia()).isEqualTo(1L);
        assertThat(response.id_insumo()).isEqualTo(2L);
        assertThat(response.id_producto()).isEqualTo(3L);
        assertThat(response.cantidadAfectada()).isEqualByComparingTo("1.500");
        assertThat(response.estado()).isEqualTo(EstadoIncidencia.ABIERTA);
        assertThat(response.fecha()).isNotNull();
        assertThat(response.id_usuario()).isEqualTo(7L);
        assertThat(response.resolucionAdoptada()).isNull();
    }

    @Test
    void resolverIncidenciaLaCierraYRegistraFecha() {
        Incidencia incidencia = incidenciaAbierta();
        when(incidenciaRepository.findById(5L)).thenReturn(Optional.of(incidencia));
        when(incidenciaRepository.save(any(Incidencia.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IncidenciaResponseDTO response = incidenciaService.resolverIncidencia(
                5L, new IncidenciaResolucionDTO("Se reemplazó la pieza", 7L));

        assertThat(response.estado()).isEqualTo(EstadoIncidencia.CERRADA);
        assertThat(response.resolucionAdoptada()).isEqualTo("Se reemplazó la pieza");
        assertThat(response.id_usuario_resolucion()).isEqualTo(7L);
        assertThat(response.fechaResolucion()).isNotNull();
    }

    @Test
    void resolverDosVecesLanzaConflicto() {
        Incidencia incidencia = incidenciaAbierta();
        when(incidenciaRepository.findById(5L)).thenReturn(Optional.of(incidencia));
        when(incidenciaRepository.save(any(Incidencia.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        incidenciaService.resolverIncidencia(
                5L, new IncidenciaResolucionDTO("Primera resolución", 7L));

        assertThatThrownBy(() -> incidenciaService.resolverIncidencia(
                5L, new IncidenciaResolucionDTO("Segunda resolución", 7L)))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("ya está cerrada");
    }

    @Test
    void resolverConResolucionVaciaLanzaError() {
        Incidencia incidencia = incidenciaAbierta();
        when(incidenciaRepository.findById(5L)).thenReturn(Optional.of(incidencia));

        assertThatThrownBy(() -> incidenciaService.resolverIncidencia(
                5L, new IncidenciaResolucionDTO("   ", 7L)))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("obligatoria");

        assertThat(incidencia.getEstado()).isEqualTo(EstadoIncidencia.ABIERTA);
        verify(incidenciaRepository, never()).save(any());
    }

    @Test
    void eliminarIncidenciaCerradaLanzaConflicto() {
        Incidencia incidencia = incidenciaAbierta();
        incidencia.setEstado(EstadoIncidencia.CERRADA);
        when(incidenciaRepository.findById(5L)).thenReturn(Optional.of(incidencia));

        assertThatThrownBy(() -> incidenciaService.eliminarIncidencia(5L))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("cerrada");

        verify(incidenciaRepository, never()).delete(any());
    }

    @Test
    void eliminarIncidenciaAbiertaLaBorra() {
        Incidencia incidencia = incidenciaAbierta();
        when(incidenciaRepository.findById(5L)).thenReturn(Optional.of(incidencia));

        incidenciaService.eliminarIncidencia(5L);

        verify(incidenciaRepository).delete(incidencia);
    }

    @Test
    void actualizarIncidenciaCerradaLanzaConflicto() {
        Incidencia incidencia = incidenciaAbierta();
        incidencia.setEstado(EstadoIncidencia.CERRADA);
        when(incidenciaRepository.findById(5L)).thenReturn(Optional.of(incidencia));

        IncidenciaUpdateDTO dto = new IncidenciaUpdateDTO(
                1L, 2L, null, BigDecimal.ONE, null, null);

        assertThatThrownBy(() -> incidenciaService.actualizarIncidencia(5L, dto))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("cerrada");

        verify(incidenciaRepository, never()).save(any());
    }

    @Test
    void obtenerIncidenciaInexistenteLanzaNoEncontrado() {
        when(incidenciaRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidenciaService.obtenerIncidencia(404L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("404");
    }

    private TipoIncidencia tipoActivo() {
        TipoIncidencia tipo = new TipoIncidencia();
        tipo.setId_tipo_incidencia(1L);
        tipo.setNombre("Rotura");
        tipo.setActivo(true);
        return tipo;
    }

    private Insumo insumo(Long id, String nombre) {
        Insumo insumo = new Insumo();
        insumo.setId_insumo(id);
        insumo.setNombre(nombre);
        insumo.setStockActual(new BigDecimal("10.000"));
        insumo.setUnidadMedida("unidad");
        insumo.setStockMinimo(1);
        insumo.setCosto(new BigDecimal("100.00"));
        return insumo;
    }

    private Producto producto(Long id, String nombre) {
        Producto producto = new Producto();
        producto.setId_producto(id);
        producto.setNombre(nombre);
        producto.setPrecio(new BigDecimal("500.00"));
        producto.setStockActual(new BigDecimal("5.000"));
        producto.setCategoria("Impresión");
        return producto;
    }

    private Incidencia incidenciaAbierta() {
        Incidencia incidencia = new Incidencia();
        incidencia.setId_incidencia(5L);
        incidencia.setTipoIncidencia(tipoActivo());
        incidencia.setInsumo(insumo(2L, "Tinta"));
        incidencia.setCantidadAfectada(new BigDecimal("1.000"));
        incidencia.setDescripcion("Rotura de tinta");
        incidencia.setEstado(EstadoIncidencia.ABIERTA);
        incidencia.setFecha(LocalDateTime.now().minusHours(1));
        incidencia.setId_usuario(7L);
        return incidencia;
    }
}

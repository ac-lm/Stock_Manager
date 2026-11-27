package com.taller.gestion.reportes.service;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.incidencias.entity.EstadoIncidencia;
import com.taller.gestion.reportes.dto.ConteoIncidenciaPorTipoDTO;
import com.taller.gestion.reportes.dto.IncidenciaDescartesFilaDTO;
import com.taller.gestion.reportes.dto.ReporteIncidenciasDescartesDTO;
import com.taller.gestion.reportes.dto.TipoArticuloStock;
import com.taller.gestion.reportes.dto.TipoIncidenciaOpcionDTO;
import com.taller.gestion.reportes.dto.TotalesIncidenciasDescartesDTO;
import com.taller.gestion.reportes.repository.DetalleIncidencia;
import com.taller.gestion.reportes.repository.ReporteIncidenciasDescartesRepository;
import com.taller.gestion.reportes.repository.ResumenDescarteIncidencia;
import com.taller.gestion.reportes.repository.TipoIncidenciaDisponible;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Incidencias del rango contra descartes MERMA del mismo artículo. No hay FK entre ambos hechos:
 * el cruce es agregado por artículo y período, y nunca se netean cantidades de unidades distintas.
 */
@Service
@RequiredArgsConstructor
public class ReporteIncidenciasDescartesService {

    public static final List<String> AVISOS = List.of(
            "No existe un vínculo 1:1 entre incidencia y descarte; INC-07 sigue pendiente.",
            "Puede haber MERMA sin incidencia e incidencia sin MERMA. Cuando coinciden se marca "
                    + "posible traslape, sin sumar ni restar.",
            "Una incidencia puede afectar producto e insumo a la vez y cerrarla hoy no descuenta stock.",
            "Cuenta como descarte solo SALIDA con motivo MERMA y sin id_venta; ENTRADA/MERMA no cuenta.",
            "Los tipos inactivos aparecen si tienen historial y se marcan como inactivos en el selector.",
            "Los usuarios de Incidencia son ids libres sin FK, distintos del usuario del movimiento.");

    private static final int MAX_ANIOS = 5;
    private static final String SIN_INCIDENCIA = "Sin incidencia";
    private static final String NO_APLICA = "—";
    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);

    private final ReporteIncidenciasDescartesRepository repositorio;

    @Transactional(readOnly = true)
    public ReporteIncidenciasDescartesDTO incidenciasDescartes(
            LocalDate desde, LocalDate hasta, Long idTipoIncidencia, String estadoTexto,
            String tipoArticuloTexto, Long idArticulo) {

        validarRango(desde, hasta);
        EstadoIncidencia estado = parsearEstado(estadoTexto);
        TipoArticuloStock tipoArticulo = parsearTipoArticulo(tipoArticuloTexto);
        if (idArticulo != null && tipoArticulo == null) {
            throw new DatosInvalidosException(
                    "El artículo requiere indicar si es producto o insumo");
        }

        LocalDateTime desdeHora = desde.atStartOfDay();
        // 'hasta' se incluye completo: el límite superior es el día siguiente, sin zonas.
        LocalDateTime hastaExclusivo = hasta.plusDays(1).atStartOfDay();

        List<TipoIncidenciaDisponible> tiposDisponibles =
                repositorio.tiposDisponibles(desdeHora, hastaExclusivo);
        List<DetalleIncidencia> detalles = repositorio.consultarIncidencias(
                desdeHora, hastaExclusivo, idTipoIncidencia,
                estado == null ? null : estado.name(), tipoArticulo, idArticulo);
        List<ResumenDescarteIncidencia> descartes = repositorio.consultarDescartes(
                desdeHora, hastaExclusivo, tipoArticulo, idArticulo);

        Map<ClaveFila, Acumulado> porFila = new LinkedHashMap<>();
        Map<Long, String> estadoPorIncidencia = new HashMap<>();
        Map<Long, DetalleIncidencia> tipoPorIncidencia = new HashMap<>();
        for (DetalleIncidencia detalle : detalles) {
            estadoPorIncidencia.putIfAbsent(detalle.idIncidencia(), detalle.estado());
            tipoPorIncidencia.putIfAbsent(detalle.idIncidencia(), detalle);
            // Al filtrar por tipo de artículo se acumula solo ese lado de una incidencia mixta.
            if (tipoArticulo != TipoArticuloStock.INSUMO && detalle.idProducto() != null) {
                acumular(porFila, detalle, TipoArticuloStock.PRODUCTO, detalle.idProducto(),
                        detalle.nombreProducto(), null);
            }
            if (tipoArticulo != TipoArticuloStock.PRODUCTO && detalle.idInsumo() != null) {
                acumular(porFila, detalle, TipoArticuloStock.INSUMO, detalle.idInsumo(),
                        detalle.nombreInsumo(), detalle.unidad());
            }
        }

        Map<ClaveArticulo, ResumenDescarteIncidencia> descartesPorArticulo = new HashMap<>();
        descartes.forEach(d -> descartesPorArticulo.put(
                new ClaveArticulo(d.tipoArticulo(), d.idArticulo()), d));

        List<IncidenciaDescartesFilaDTO> filas = new ArrayList<>();
        Set<ClaveArticulo> descartesYaAsignados = new HashSet<>();
        porFila.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(comparadorClave()))
                .forEach(entry -> {
                    ClaveFila clave = entry.getKey();
                    Acumulado acumulado = entry.getValue();
                    ClaveArticulo articulo = new ClaveArticulo(clave.tipoArticulo(), clave.idArticulo());
                    ResumenDescarteIncidencia descarte = descartesPorArticulo.get(articulo);
                    long movimientos = 0L;
                    BigDecimal cantidadDescartada = CERO;
                    // Cada descarte se imprime una sola vez: no se reparte entre tipos ni estados.
                    if (descarte != null && descartesYaAsignados.add(articulo)) {
                        movimientos = descarte.movimientos();
                        cantidadDescartada = descarte.cantidad();
                    }
                    filas.add(new IncidenciaDescartesFilaDTO(
                            clave.idTipo(), clave.nombreTipo(), clave.estado(),
                            clave.tipoArticulo(), clave.idArticulo(), clave.articulo(),
                            clave.unidad(), acumulado.incidencias,
                            normalizar(acumulado.cantidad), acumulado.abiertas,
                            acumulado.cerradas, movimientos, normalizar(cantidadDescartada),
                            acumulado.incidencias > 0 && movimientos > 0));
                });

        descartes.stream()
                .filter(d -> !descartesYaAsignados.contains(
                        new ClaveArticulo(d.tipoArticulo(), d.idArticulo())))
                .sorted(Comparator.comparing(ResumenDescarteIncidencia::nombre,
                        Comparator.nullsLast(String::compareTo)))
                .forEach(d -> filas.add(new IncidenciaDescartesFilaDTO(
                        null, SIN_INCIDENCIA, NO_APLICA, d.tipoArticulo(), d.idArticulo(),
                        d.nombre(), d.unidad(), 0L, CERO, 0L, 0L, d.movimientos(),
                        normalizar(d.cantidad()), false)));

        agregarFilaDeTipoVacio(filas, idTipoIncidencia, tiposDisponibles);
        filas.sort(Comparator
                .comparing(IncidenciaDescartesFilaDTO::tipoIncidencia,
                        Comparator.nullsLast(String::compareTo))
                .thenComparing(fila -> fila.articulo() == null ? "" : fila.articulo())
                .thenComparing(fila -> fila.estado() == null ? "" : fila.estado()));

        long abiertas = estadoPorIncidencia.values().stream()
                .filter(EstadoIncidencia.ABIERTA.name()::equals).count();
        long cerradas = estadoPorIncidencia.values().stream()
                .filter(EstadoIncidencia.CERRADA.name()::equals).count();
        Set<ClaveArticulo> articulos = new HashSet<>();
        filas.stream()
                .filter(fila -> fila.tipoArticulo() != null)
                .forEach(fila -> articulos.add(
                        new ClaveArticulo(fila.tipoArticulo(), fila.idArticulo())));

        // Una incidencia con producto e insumo aparece en dos filas: aquí se cuenta una sola vez.
        Map<Long, Long> conteoPorTipo = new LinkedHashMap<>();
        Map<Long, String> nombrePorTipo = new HashMap<>();
        estadoPorIncidencia.keySet().forEach(id -> {
            DetalleIncidencia detalle = tipoPorIncidencia.get(id);
            if (detalle == null) {
                return;
            }
            nombrePorTipo.putIfAbsent(detalle.idTipoIncidencia(), detalle.nombreTipo());
            conteoPorTipo.merge(detalle.idTipoIncidencia(), 1L, Long::sum);
        });
        List<ConteoIncidenciaPorTipoDTO> incidenciasPorTipo = conteoPorTipo.entrySet().stream()
                .map(entry -> new ConteoIncidenciaPorTipoDTO(
                        entry.getKey(), nombrePorTipo.get(entry.getKey()), entry.getValue()))
                .sorted(Comparator.comparing(ConteoIncidenciaPorTipoDTO::tipoIncidencia))
                .toList();

        return new ReporteIncidenciasDescartesDTO(
                desde, hasta, idTipoIncidencia,
                estado == null ? "TODOS" : estado.name(),
                tipoArticulo == null ? "TODOS" : tipoArticulo.name(), idArticulo,
                tiposDisponibles.stream()
                        .map(t -> new TipoIncidenciaOpcionDTO(t.id(), t.nombre(), t.activo()))
                        .toList(),
                List.copyOf(filas),
                new TotalesIncidenciasDescartesDTO(
                        estadoPorIncidencia.size(), abiertas, cerradas,
                        descartes.stream().mapToLong(ResumenDescarteIncidencia::movimientos).sum(),
                        articulos.size()),
                incidenciasPorTipo,
                AVISOS);
    }

    private void acumular(Map<ClaveFila, Acumulado> porFila, DetalleIncidencia detalle,
                          TipoArticuloStock tipoArticulo, long idArticulo,
                          String articulo, String unidad) {
        ClaveFila clave = new ClaveFila(
                detalle.idTipoIncidencia(), detalle.nombreTipo(), detalle.activoTipo(),
                detalle.estado(), tipoArticulo, idArticulo, articulo, unidad);
        porFila.computeIfAbsent(clave, ignored -> new Acumulado()).sumar(detalle);
    }

    private void agregarFilaDeTipoVacio(List<IncidenciaDescartesFilaDTO> filas, Long idTipo,
                                        List<TipoIncidenciaDisponible> tiposDisponibles) {
        if (idTipo == null || filas.stream().anyMatch(f -> idTipo.equals(f.id_tipo_incidencia()))) {
            return;
        }
        String nombre = tiposDisponibles.stream()
                .filter(t -> t.id() == idTipo)
                .map(TipoIncidenciaDisponible::nombre)
                .findFirst()
                .orElse("Tipo #" + idTipo);
        filas.add(new IncidenciaDescartesFilaDTO(
                idTipo, nombre, NO_APLICA, null, 0L, NO_APLICA, null,
                0L, CERO, 0L, 0L, 0L, CERO, false));
    }

    private Comparator<ClaveFila> comparadorClave() {
        return Comparator.comparing(ClaveFila::nombreTipo, Comparator.nullsLast(String::compareTo))
                .thenComparing(clave -> clave.articulo() == null ? "" : clave.articulo())
                .thenComparing(clave -> clave.estado() == null ? "" : clave.estado());
    }

    private BigDecimal normalizar(BigDecimal valor) {
        return valor == null ? CERO : valor.setScale(3, RoundingMode.HALF_UP);
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new DatosInvalidosException("El rango de fechas es obligatorio");
        }
        if (desde.isAfter(hasta)) {
            throw new DatosInvalidosException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
        if (hasta.isAfter(desde.plusYears(MAX_ANIOS).minusDays(1))) {
            throw new DatosInvalidosException("El rango no puede superar los "
                    + MAX_ANIOS + " años");
        }
    }

    private EstadoIncidencia parsearEstado(String valor) {
        String texto = valor == null || valor.isBlank() ? "TODOS" : valor.trim().toUpperCase(Locale.ROOT);
        if ("TODOS".equals(texto)) {
            return null;
        }
        try {
            return EstadoIncidencia.valueOf(texto);
        } catch (IllegalArgumentException ex) {
            throw new DatosInvalidosException("El parámetro 'estado' tiene un valor inválido");
        }
    }

    private TipoArticuloStock parsearTipoArticulo(String valor) {
        String texto = valor == null || valor.isBlank() ? "TODOS" : valor.trim().toUpperCase(Locale.ROOT);
        if ("TODOS".equals(texto)) {
            return null;
        }
        try {
            return TipoArticuloStock.valueOf(texto);
        } catch (IllegalArgumentException ex) {
            throw new DatosInvalidosException("El parámetro 'tipo_articulo' tiene un valor inválido");
        }
    }

    private record ClaveFila(Long idTipo, String nombreTipo, boolean activoTipo, String estado,
                             TipoArticuloStock tipoArticulo, long idArticulo, String articulo,
                             String unidad) {
    }

    private record ClaveArticulo(TipoArticuloStock tipoArticulo, long idArticulo) {
    }

    private static class Acumulado {
        private long incidencias;
        private BigDecimal cantidad = CERO;
        private long abiertas;
        private long cerradas;

        private void sumar(DetalleIncidencia detalle) {
            incidencias++;
            cantidad = cantidad.add(detalle.cantidad());
            if (EstadoIncidencia.ABIERTA.name().equals(detalle.estado())) {
                abiertas++;
            } else if (EstadoIncidencia.CERRADA.name().equals(detalle.estado())) {
                cerradas++;
            }
        }
    }
}

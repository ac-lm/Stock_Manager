package com.taller.gestion.caja.service;

import com.taller.gestion.caja.dto.AbrirCajaDTO;
import com.taller.gestion.caja.dto.CajaActualDTO;
import com.taller.gestion.caja.dto.CerrarCajaDTO;
import com.taller.gestion.caja.dto.MovimientoCajaCreateDTO;
import com.taller.gestion.caja.dto.MovimientoCajaResponseDTO;
import com.taller.gestion.caja.dto.RevisionCajaDTO;
import com.taller.gestion.caja.dto.SesionCajaDetalleDTO;
import com.taller.gestion.caja.dto.SesionCajaResponseDTO;
import com.taller.gestion.caja.dto.TotalesCajaDTO;
import com.taller.gestion.caja.entity.CierreCajaDetalle;
import com.taller.gestion.caja.entity.CierreCajaDetalleId;
import com.taller.gestion.caja.entity.ConceptoMovimientoCaja;
import com.taller.gestion.caja.entity.EstadoSesionCaja;
import com.taller.gestion.caja.entity.MedioPago;
import com.taller.gestion.caja.entity.MovimientoCaja;
import com.taller.gestion.caja.entity.SesionCaja;
import com.taller.gestion.caja.entity.TipoMovimientoCaja;
import com.taller.gestion.caja.repository.CierreCajaDetalleRepository;
import com.taller.gestion.caja.repository.MovimientoCajaRepository;
import com.taller.gestion.caja.repository.SesionCajaRepository;
import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.common.exception.OperacionNoPermitidaException;
import com.taller.gestion.common.exception.RecursoNoEncontradoException;
import com.taller.gestion.usuario.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CajaService {

    /**
     * Clave arbitraria y estable para el advisory lock de apertura. Se usa siempre la misma
     * para que dos aperturas concurrentes se serialicen.
     */
    private static final long CLAVE_BLOQUEO_APERTURA = 7401L;
    private static final BigDecimal CERO = new BigDecimal("0.00");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final SesionCajaRepository sesionCajaRepository;
    private final MovimientoCajaRepository movimientoCajaRepository;
    private final CierreCajaDetalleRepository cierreCajaDetalleRepository;
    private final UsuarioService usuarioService;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public CajaActualDTO actual() {
        SesionCaja abierta = sesionCajaRepository
                .findFirstByEstadoOrderByFechaAperturaDescHoraAperturaDesc(EstadoSesionCaja.ABIERTA)
                .orElse(null);

        List<MovimientoCajaResponseDTO> movimientos = abierta == null
                ? List.of()
                : movimientoCajaRepository.listarPorSesion(abierta.getId_sesion()).stream()
                        .map(this::mapearMovimiento)
                        .toList();

        return new CajaActualDTO(
                abierta == null ? null : mapearSesion(abierta),
                calcularMontoInicialSugerido(),
                movimientos);
    }

    @Transactional
    public SesionCajaResponseDTO abrir(AbrirCajaDTO dto) {
        bloquearApertura();

        SesionCaja abierta = sesionCajaRepository
                .findFirstByEstadoOrderByFechaAperturaDescHoraAperturaDesc(EstadoSesionCaja.ABIERTA)
                .orElse(null);
        if (abierta != null) {
            throw new OperacionNoPermitidaException(
                    "Ya hay una caja abierta por " + abierta.getUsuarioApertura().getNombre()
                            + " el " + formatearFechaHora(
                                    abierta.getFechaApertura(), abierta.getHoraApertura()));
        }

        BigDecimal monto = normalizarMonto(dto.montoInicial());
        BigDecimal sugerido = calcularMontoInicialSugerido();
        if (monto.compareTo(sugerido) != 0 && esBlanco(dto.motivoAjusteInicial())) {
            throw new DatosInvalidosException(
                    "El motivo es obligatorio cuando el monto inicial difiere del sugerido ($"
                            + sugerido.toPlainString() + ")");
        }

        SesionCaja sesion = new SesionCaja();
        sesion.setUsuarioApertura(usuarioService.obtenerAutenticado());
        sesion.setFechaApertura(LocalDate.now());
        sesion.setHoraApertura(LocalTime.now());
        sesion.setMontoInicial(monto);
        sesion.setMotivoAjusteInicial(limpiar(dto.motivoAjusteInicial()));
        sesion.setEstado(EstadoSesionCaja.ABIERTA);

        return mapearSesion(sesionCajaRepository.save(sesion));
    }

    @Transactional
    public MovimientoCajaResponseDTO registrarEgreso(Long id_sesion, MovimientoCajaCreateDTO dto) {
        SesionCaja sesion = obtenerSesionParaEscritura(id_sesion);
        if (sesion.getEstado() != EstadoSesionCaja.ABIERTA) {
            throw new OperacionNoPermitidaException("La sesión de caja no está abierta");
        }
        if (dto.concepto() != ConceptoMovimientoCaja.RETIRO
                && dto.concepto() != ConceptoMovimientoCaja.GASTO) {
            throw new DatosInvalidosException("El concepto debe ser RETIRO o GASTO");
        }

        BigDecimal monto = normalizarMonto(dto.monto());
        if (monto.signum() <= 0) {
            throw new DatosInvalidosException("El monto debe ser mayor a cero");
        }

        if (dto.medioPago() == MedioPago.EFECTIVO) {
            BigDecimal esperado = calcularEsperadoAbierto(sesion, MedioPago.EFECTIVO);
            if (monto.compareTo(esperado) > 0) {
                throw new OperacionNoPermitidaException(
                        "El egreso en efectivo no puede superar el efectivo esperado ($"
                                + esperado.toPlainString() + ")");
            }
        }

        MovimientoCaja movimiento = new MovimientoCaja();
        movimiento.setSesionCaja(sesion);
        movimiento.setUsuario(usuarioService.obtenerAutenticado());
        movimiento.setTipo(TipoMovimientoCaja.EGRESO);
        movimiento.setConcepto(dto.concepto());
        movimiento.setMonto(monto);
        movimiento.setMedioPago(dto.medioPago());
        movimiento.setFecha(LocalDate.now());
        movimiento.setHora(LocalTime.now());
        movimiento.setObservaciones(limpiar(dto.observaciones()));
        movimiento.setId_costo(dto.id_costo());

        return mapearMovimiento(movimientoCajaRepository.save(movimiento));
    }

    @Transactional
    public SesionCajaResponseDTO cerrar(Long id_sesion, CerrarCajaDTO dto) {
        SesionCaja sesion = obtenerSesionParaEscritura(id_sesion);
        if (sesion.getEstado() != EstadoSesionCaja.ABIERTA) {
            throw new OperacionNoPermitidaException("La sesión de caja ya está cerrada");
        }

        BigDecimal esperadoEfectivo = calcularEsperadoAbierto(sesion, MedioPago.EFECTIVO);
        BigDecimal esperadoTransferencia = calcularEsperadoAbierto(sesion, MedioPago.TRANSFERENCIA);
        BigDecimal declaradoEfectivo = normalizarMonto(dto.declaradoEfectivo());
        BigDecimal declaradoTransferencia = normalizarMonto(dto.declaradoTransferencia());

        BigDecimal diferenciaEfectivo = declaradoEfectivo.subtract(esperadoEfectivo);
        BigDecimal diferenciaTransferencia = declaradoTransferencia.subtract(esperadoTransferencia);
        boolean hayDiferencia = diferenciaEfectivo.signum() != 0
                || diferenciaTransferencia.signum() != 0;

        if (hayDiferencia && esBlanco(dto.observaciones())) {
            throw new DatosInvalidosException(
                    "La observación es obligatoria cuando alguna diferencia no es cero");
        }

        guardarDetalle(sesion, MedioPago.EFECTIVO, esperadoEfectivo,
                declaradoEfectivo, diferenciaEfectivo);
        guardarDetalle(sesion, MedioPago.TRANSFERENCIA, esperadoTransferencia,
                declaradoTransferencia, diferenciaTransferencia);

        sesion.setUsuarioCierre(usuarioService.obtenerAutenticado());
        sesion.setFechaCierre(LocalDate.now());
        sesion.setHoraCierre(LocalTime.now());
        sesion.setTotalEsperado(normalizarMonto(esperadoEfectivo.add(esperadoTransferencia)));
        sesion.setTotalDeclarado(normalizarMonto(declaradoEfectivo.add(declaradoTransferencia)));
        sesion.setDiferencia(normalizarMonto(
                sesion.getTotalDeclarado().subtract(sesion.getTotalEsperado())));
        sesion.setObservaciones(limpiar(dto.observaciones()));
        sesion.setEstado(hayDiferencia
                ? EstadoSesionCaja.PENDIENTE_REVISION
                : EstadoSesionCaja.CERRADA);

        return mapearSesion(sesionCajaRepository.save(sesion));
    }

    @Transactional
    public SesionCajaResponseDTO revisar(Long id_sesion, RevisionCajaDTO dto) {
        SesionCaja sesion = obtenerSesionParaEscritura(id_sesion);
        if (sesion.getEstado() != EstadoSesionCaja.PENDIENTE_REVISION) {
            throw new OperacionNoPermitidaException("La sesión no está pendiente de revisión");
        }
        if (esBlanco(dto.observacion())) {
            throw new DatosInvalidosException("La observación de revisión es obligatoria");
        }

        sesion.setUsuarioRevision(usuarioService.obtenerAutenticado());
        sesion.setFechaRevision(LocalDateTime.now());
        sesion.setObservacionRevision(dto.observacion().trim());
        sesion.setEstado(EstadoSesionCaja.CERRADA);

        return mapearSesion(sesionCajaRepository.save(sesion));
    }

    @Transactional(readOnly = true)
    public Page<SesionCajaResponseDTO> listar(
            EstadoSesionCaja estado,
            Long id_usuario,
            LocalDate desde,
            LocalDate hasta,
            Pageable pageable) {
        return sesionCajaRepository.buscar(estado, id_usuario, desde, hasta, pageable)
                .map(this::mapearSesion);
    }

    /** Consulta usada por costos para impedir la anulación de un costo ya pagado desde caja. */
    @Transactional(readOnly = true)
    public boolean existeEgresoVinculadoACosto(Long id_costo) {
        return movimientoCajaRepository.existePorCosto(id_costo);
    }

    @Transactional(readOnly = true)
    public SesionCajaDetalleDTO obtenerDetalle(Long id_sesion) {
        SesionCaja sesion = sesionCajaRepository.findById(id_sesion)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Sesión de caja no encontrada con id " + id_sesion));

        List<MovimientoCajaResponseDTO> movimientos = movimientoCajaRepository
                .listarPorSesion(id_sesion).stream()
                .map(this::mapearMovimiento)
                .toList();

        return new SesionCajaDetalleDTO(mapearSesion(sesion), movimientos);
    }

    private SesionCaja obtenerSesionParaEscritura(Long id_sesion) {
        return sesionCajaRepository.findByIdForUpdate(id_sesion)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Sesión de caja no encontrada con id " + id_sesion));
    }

    private BigDecimal calcularMontoInicialSugerido() {
        return sesionCajaRepository
                .buscarUltimoCierre(
                        List.of(EstadoSesionCaja.CERRADA, EstadoSesionCaja.PENDIENTE_REVISION))
                .flatMap(sesion -> cierreCajaDetalleRepository
                        .buscarPorSesionYMedio(sesion.getId_sesion(), MedioPago.EFECTIVO))
                .map(CierreCajaDetalle::getTotalDeclarado)
                .map(this::normalizarMonto)
                .orElse(CERO);
    }

    private BigDecimal calcularEsperadoAbierto(SesionCaja sesion, MedioPago medioPago) {
        BigDecimal ingresos = movimientoCajaRepository.sumarPorTipoYMedio(
                sesion.getId_sesion(), medioPago, TipoMovimientoCaja.INGRESO);
        BigDecimal egresos = movimientoCajaRepository.sumarPorTipoYMedio(
                sesion.getId_sesion(), medioPago, TipoMovimientoCaja.EGRESO);

        BigDecimal esperado = normalizarMonto(ingresos).subtract(normalizarMonto(egresos));
        if (medioPago == MedioPago.EFECTIVO) {
            esperado = esperado.add(normalizarMonto(sesion.getMontoInicial()));
        }
        return normalizarMonto(esperado);
    }

    private TotalesCajaDTO calcularTotales(SesionCaja sesion) {
        if (sesion.getEstado() == EstadoSesionCaja.ABIERTA) {
            BigDecimal esperadoEfectivo = calcularEsperadoAbierto(sesion, MedioPago.EFECTIVO);
            BigDecimal esperadoTransferencia = calcularEsperadoAbierto(
                    sesion, MedioPago.TRANSFERENCIA);
            return new TotalesCajaDTO(
                    esperadoEfectivo,
                    esperadoTransferencia,
                    null, null, null, null,
                    normalizarMonto(esperadoEfectivo.add(esperadoTransferencia)),
                    null,
                    null);
        }

        BigDecimal esperadoEfectivo = CERO;
        BigDecimal esperadoTransferencia = CERO;
        BigDecimal declaradoEfectivo = CERO;
        BigDecimal declaradoTransferencia = CERO;
        BigDecimal diferenciaEfectivo = CERO;
        BigDecimal diferenciaTransferencia = CERO;

        for (CierreCajaDetalle detalle : cierreCajaDetalleRepository.listarPorSesion(
                sesion.getId_sesion())) {
            if (detalle.getId().getMedioPago() == MedioPago.EFECTIVO) {
                esperadoEfectivo = normalizarMonto(detalle.getTotalEsperado());
                declaradoEfectivo = normalizarMonto(detalle.getTotalDeclarado());
                diferenciaEfectivo = normalizarMonto(detalle.getDiferencia());
            } else if (detalle.getId().getMedioPago() == MedioPago.TRANSFERENCIA) {
                esperadoTransferencia = normalizarMonto(detalle.getTotalEsperado());
                declaradoTransferencia = normalizarMonto(detalle.getTotalDeclarado());
                diferenciaTransferencia = normalizarMonto(detalle.getDiferencia());
            }
        }

        BigDecimal totalEsperado = sesion.getTotalEsperado() != null
                ? normalizarMonto(sesion.getTotalEsperado())
                : normalizarMonto(esperadoEfectivo.add(esperadoTransferencia));
        BigDecimal totalDeclarado = sesion.getTotalDeclarado() != null
                ? normalizarMonto(sesion.getTotalDeclarado())
                : normalizarMonto(declaradoEfectivo.add(declaradoTransferencia));
        BigDecimal diferencia = sesion.getDiferencia() != null
                ? normalizarMonto(sesion.getDiferencia())
                : normalizarMonto(totalDeclarado.subtract(totalEsperado));

        return new TotalesCajaDTO(
                esperadoEfectivo,
                esperadoTransferencia,
                declaradoEfectivo,
                declaradoTransferencia,
                diferenciaEfectivo,
                diferenciaTransferencia,
                totalEsperado,
                totalDeclarado,
                diferencia);
    }

    private SesionCajaResponseDTO mapearSesion(SesionCaja sesion) {
        return new SesionCajaResponseDTO(
                sesion.getId_sesion(),
                sesion.getUsuarioApertura().getId_usuario(),
                sesion.getUsuarioApertura().getNombre(),
                sesion.getFechaApertura(),
                sesion.getHoraApertura(),
                sesion.getUsuarioCierre() != null
                        ? sesion.getUsuarioCierre().getId_usuario() : null,
                sesion.getUsuarioCierre() != null
                        ? sesion.getUsuarioCierre().getNombre() : null,
                sesion.getFechaCierre(),
                sesion.getHoraCierre(),
                normalizarMonto(sesion.getMontoInicial()),
                sesion.getMotivoAjusteInicial(),
                sesion.getEstado(),
                calcularTotales(sesion),
                sesion.getObservaciones(),
                sesion.getUsuarioRevision() != null
                        ? sesion.getUsuarioRevision().getId_usuario() : null,
                sesion.getUsuarioRevision() != null
                        ? sesion.getUsuarioRevision().getNombre() : null,
                sesion.getFechaRevision(),
                sesion.getObservacionRevision());
    }

    private MovimientoCajaResponseDTO mapearMovimiento(MovimientoCaja movimiento) {
        return new MovimientoCajaResponseDTO(
                movimiento.getId_movimiento_caja(),
                movimiento.getSesionCaja().getId_sesion(),
                movimiento.getUsuario().getId_usuario(),
                movimiento.getUsuario().getNombre(),
                movimiento.getId_venta(),
                movimiento.getId_costo(),
                movimiento.getTipo(),
                movimiento.getConcepto(),
                normalizarMonto(movimiento.getMonto()),
                movimiento.getMedioPago(),
                movimiento.getFecha(),
                movimiento.getHora(),
                movimiento.getObservaciones());
    }

    private void guardarDetalle(
            SesionCaja sesion,
            MedioPago medioPago,
            BigDecimal esperado,
            BigDecimal declarado,
            BigDecimal diferencia) {
        CierreCajaDetalle detalle = new CierreCajaDetalle();
        detalle.setId(new CierreCajaDetalleId(sesion.getId_sesion(), medioPago));
        detalle.setSesionCaja(sesion);
        detalle.setTotalEsperado(esperado);
        detalle.setTotalDeclarado(declarado);
        detalle.setDiferencia(diferencia);
        cierreCajaDetalleRepository.save(detalle);
    }

    private void bloquearApertura() {
        // El lock se libera al terminar la transacción. Serializa el "verificar y crear"
        // para que dos aperturas concurrentes no dejen dos sesiones ABIERTA.
        jdbcTemplate.execute("SELECT pg_advisory_xact_lock(" + CLAVE_BLOQUEO_APERTURA + ")");
    }

    private BigDecimal normalizarMonto(BigDecimal monto) {
        return monto == null ? CERO : monto.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean esBlanco(String texto) {
        return texto == null || texto.isBlank();
    }

    private String limpiar(String texto) {
        return esBlanco(texto) ? null : texto.trim();
    }

    private String formatearFechaHora(LocalDate fecha, LocalTime hora) {
        String fechaTexto = fecha != null ? fecha.format(FORMATO_FECHA) : "—";
        String horaTexto = hora != null ? hora.format(FORMATO_HORA) : "";
        return (fechaTexto + " " + horaTexto).trim();
    }
}

package com.taller.gestion.caja.service;

import com.taller.gestion.caja.dto.AbrirCajaDTO;
import com.taller.gestion.caja.dto.CajaActualDTO;
import com.taller.gestion.caja.dto.CerrarCajaDTO;
import com.taller.gestion.caja.dto.MovimientoCajaCreateDTO;
import com.taller.gestion.caja.dto.MovimientoCajaResponseDTO;
import com.taller.gestion.caja.dto.RevisionCajaDTO;
import com.taller.gestion.caja.dto.SesionCajaResponseDTO;
import com.taller.gestion.caja.entity.CierreCajaDetalle;
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
import com.taller.gestion.usuario.entity.Rol;
import com.taller.gestion.usuario.entity.Usuario;
import com.taller.gestion.usuario.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CajaServiceTest {

    private static final Long ID_SESION = 1L;

    @Mock
    private SesionCajaRepository sesionCajaRepository;
    @Mock
    private MovimientoCajaRepository movimientoCajaRepository;
    @Mock
    private CierreCajaDetalleRepository cierreCajaDetalleRepository;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private CajaService cajaService;

    private Usuario usuario(Long id, String nombre, Rol rol) {
        Usuario u = new Usuario();
        u.setId_usuario(id);
        u.setNombre(nombre);
        u.setEmail(nombre.toLowerCase() + "@taller.local");
        u.setPassword("x");
        u.setRol(rol);
        u.setActivo(true);
        return u;
    }

    private SesionCaja sesion(EstadoSesionCaja estado) {
        SesionCaja s = new SesionCaja();
        s.setId_sesion(ID_SESION);
        s.setUsuarioApertura(usuario(5L, "Ana", Rol.VENDEDOR));
        s.setFechaApertura(LocalDate.of(2026, 11, 20));
        s.setHoraApertura(LocalTime.of(9, 0));
        s.setMontoInicial(new BigDecimal("1000.00"));
        s.setEstado(estado);
        return s;
    }

    private void prepararAperturaSinHistorial() {
        when(sesionCajaRepository.findFirstByEstadoOrderByFechaAperturaDescHoraAperturaDesc(
                EstadoSesionCaja.ABIERTA)).thenReturn(Optional.empty());
        when(sesionCajaRepository.buscarUltimoCierre(any()))
                .thenReturn(Optional.empty());
    }

    private void prepararEgresoEfectivo(BigDecimal esperado) {
        when(sesionCajaRepository.findByIdForUpdate(ID_SESION))
                .thenReturn(Optional.of(sesion(EstadoSesionCaja.ABIERTA)));
        when(movimientoCajaRepository.sumarPorTipoYMedio(
                ID_SESION, MedioPago.EFECTIVO, TipoMovimientoCaja.INGRESO))
                .thenReturn(BigDecimal.ZERO);
        when(movimientoCajaRepository.sumarPorTipoYMedio(
                ID_SESION, MedioPago.EFECTIVO, TipoMovimientoCaja.EGRESO))
                .thenReturn(BigDecimal.ZERO);
    }

    private void prepararCierre(BigDecimal ingresosEfectivo, BigDecimal egresosEfectivo,
                                BigDecimal ingresosTransferencia, BigDecimal egresosTransferencia) {
        when(sesionCajaRepository.findByIdForUpdate(ID_SESION))
                .thenReturn(Optional.of(sesion(EstadoSesionCaja.ABIERTA)));
        when(movimientoCajaRepository.sumarPorTipoYMedio(
                ID_SESION, MedioPago.EFECTIVO, TipoMovimientoCaja.INGRESO))
                .thenReturn(ingresosEfectivo);
        when(movimientoCajaRepository.sumarPorTipoYMedio(
                ID_SESION, MedioPago.EFECTIVO, TipoMovimientoCaja.EGRESO))
                .thenReturn(egresosEfectivo);
        when(movimientoCajaRepository.sumarPorTipoYMedio(
                ID_SESION, MedioPago.TRANSFERENCIA, TipoMovimientoCaja.INGRESO))
                .thenReturn(ingresosTransferencia);
        when(movimientoCajaRepository.sumarPorTipoYMedio(
                ID_SESION, MedioPago.TRANSFERENCIA, TipoMovimientoCaja.EGRESO))
                .thenReturn(egresosTransferencia);
    }

    // ============================ APERTURA ============================

    @Test
    @DisplayName("Abrir la primera sesión: queda ABIERTA con el usuario de la sesión")
    void abrir_primeraSesion() {
        prepararAperturaSinHistorial();
        when(usuarioService.obtenerAutenticado())
                .thenReturn(usuario(5L, "Ana", Rol.VENDEDOR));
        when(sesionCajaRepository.save(any(SesionCaja.class))).thenAnswer(inv -> {
            SesionCaja s = inv.getArgument(0);
            s.setId_sesion(ID_SESION);
            return s;
        });

        SesionCajaResponseDTO r = cajaService.abrir(
                new AbrirCajaDTO(new BigDecimal("0.00"), null));

        assertThat(r.id_sesion()).isEqualTo(ID_SESION);
        assertThat(r.estado()).isEqualTo(EstadoSesionCaja.ABIERTA);
        assertThat(r.usuarioApertura()).isEqualTo("Ana");
        assertThat(r.montoInicial()).isEqualByComparingTo("0.00");
        assertThat(r.fechaApertura()).isNotNull();
        assertThat(r.horaApertura()).isNotNull();
        assertThat(r.totales().esperadoEfectivo()).isEqualByComparingTo("0.00");
        verify(jdbcTemplate).execute(contains("pg_advisory_xact_lock"));
    }

    @Test
    @DisplayName("Abrir con un monto distinto al sugerido sin motivo: falla")
    void abrir_montoDistintoSinMotivo_falla() {
        prepararAperturaSinHistorial();

        assertThatThrownBy(() -> cajaService.abrir(
                new AbrirCajaDTO(new BigDecimal("100.00"), " ")))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("motivo");
        verify(sesionCajaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Abrir con un monto distinto y motivo: guarda el motivo")
    void abrir_montoDistintoConMotivo() {
        prepararAperturaSinHistorial();
        when(usuarioService.obtenerAutenticado())
                .thenReturn(usuario(5L, "Ana", Rol.VENDEDOR));
        when(sesionCajaRepository.save(any(SesionCaja.class))).thenAnswer(inv -> {
            SesionCaja s = inv.getArgument(0);
            s.setId_sesion(ID_SESION);
            return s;
        });

        SesionCajaResponseDTO r = cajaService.abrir(
                new AbrirCajaDTO(new BigDecimal("100.00"), "Quedó cambio de la feria"));

        assertThat(r.montoInicial()).isEqualByComparingTo("100.00");
        assertThat(r.motivoAjusteInicial()).isEqualTo("Quedó cambio de la feria");
    }

    @Test
    @DisplayName("Abrir con una sesión ya abierta: falla y avisa quién la abrió")
    void abrir_yaHaySesionAbierta_falla() {
        SesionCaja abierta = sesion(EstadoSesionCaja.ABIERTA);
        when(sesionCajaRepository.findFirstByEstadoOrderByFechaAperturaDescHoraAperturaDesc(
                EstadoSesionCaja.ABIERTA)).thenReturn(Optional.of(abierta));

        assertThatThrownBy(() -> cajaService.abrir(
                new AbrirCajaDTO(new BigDecimal("1000.00"), null)))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("Ana")
                .hasMessageContaining("20/11/2026");
    }

    @Test
    @DisplayName("La sugerencia es el declarado en efectivo del último cierre")
    void actual_sugerenciaDelUltimoCierre() {
        SesionCaja ultima = sesion(EstadoSesionCaja.CERRADA);
        CierreCajaDetalle detalle = new CierreCajaDetalle();
        detalle.setTotalDeclarado(new BigDecimal("250.00"));
        when(sesionCajaRepository.findFirstByEstadoOrderByFechaAperturaDescHoraAperturaDesc(
                EstadoSesionCaja.ABIERTA)).thenReturn(Optional.empty());
        when(sesionCajaRepository.buscarUltimoCierre(any()))
                .thenReturn(Optional.of(ultima));
        when(cierreCajaDetalleRepository.buscarPorSesionYMedio(
                ID_SESION, MedioPago.EFECTIVO)).thenReturn(Optional.of(detalle));

        CajaActualDTO actual = cajaService.actual();

        assertThat(actual.sesion()).isNull();
        assertThat(actual.montoInicialSugerido()).isEqualByComparingTo("250.00");
        assertThat(actual.movimientos()).isEmpty();
    }

    @Test
    @DisplayName("Sin cierres anteriores, la sugerencia es cero")
    void actual_sinCierres_sugiereCero() {
        when(sesionCajaRepository.findFirstByEstadoOrderByFechaAperturaDescHoraAperturaDesc(
                EstadoSesionCaja.ABIERTA)).thenReturn(Optional.empty());
        when(sesionCajaRepository.buscarUltimoCierre(any()))
                .thenReturn(Optional.empty());

        CajaActualDTO actual = cajaService.actual();

        assertThat(actual.montoInicialSugerido()).isEqualByComparingTo("0.00");
    }

    // ============================ EGRESOS ============================

    @Test
    @DisplayName("Egreso en efectivo dentro del esperado: se registra como RETIRO/EGRESO")
    void egreso_efectivoDentroDelLimite() {
        prepararEgresoEfectivo(new BigDecimal("1000.00"));
        when(usuarioService.obtenerAutenticado())
                .thenReturn(usuario(5L, "Ana", Rol.VENDEDOR));
        when(movimientoCajaRepository.save(any(MovimientoCaja.class))).thenAnswer(inv -> {
            MovimientoCaja m = inv.getArgument(0);
            m.setId_movimiento_caja(10L);
            return m;
        });

        MovimientoCajaResponseDTO r = cajaService.registrarEgreso(ID_SESION,
                new MovimientoCajaCreateDTO(ConceptoMovimientoCaja.RETIRO, MedioPago.EFECTIVO,
                        new BigDecimal("200.00"), "Retiro para depositar"));

        assertThat(r.id_movimiento_caja()).isEqualTo(10L);
        assertThat(r.id_sesion()).isEqualTo(ID_SESION);
        assertThat(r.usuario()).isEqualTo("Ana");
        assertThat(r.tipo()).isEqualTo(TipoMovimientoCaja.EGRESO);
        assertThat(r.concepto()).isEqualTo(ConceptoMovimientoCaja.RETIRO);
        assertThat(r.medioPago()).isEqualTo(MedioPago.EFECTIVO);
        assertThat(r.monto()).isEqualByComparingTo("200.00");
        assertThat(r.observaciones()).isEqualTo("Retiro para depositar");
    }

    @Test
    @DisplayName("Egreso en efectivo mayor al esperado: falla")
    void egreso_efectivoSuperaLimite_falla() {
        prepararEgresoEfectivo(new BigDecimal("1000.00"));

        assertThatThrownBy(() -> cajaService.registrarEgreso(ID_SESION,
                new MovimientoCajaCreateDTO(ConceptoMovimientoCaja.GASTO, MedioPago.EFECTIVO,
                        new BigDecimal("1000.01"), null)))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("no puede superar");
        verify(movimientoCajaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Egreso por transferencia: no tiene límite contra el esperado")
    void egreso_transferenciaSinLimite() {
        when(sesionCajaRepository.findByIdForUpdate(ID_SESION))
                .thenReturn(Optional.of(sesion(EstadoSesionCaja.ABIERTA)));
        when(usuarioService.obtenerAutenticado())
                .thenReturn(usuario(5L, "Ana", Rol.VENDEDOR));
        when(movimientoCajaRepository.save(any(MovimientoCaja.class))).thenAnswer(inv -> {
            MovimientoCaja m = inv.getArgument(0);
            m.setId_movimiento_caja(11L);
            return m;
        });

        MovimientoCajaResponseDTO r = cajaService.registrarEgreso(ID_SESION,
                new MovimientoCajaCreateDTO(ConceptoMovimientoCaja.GASTO, MedioPago.TRANSFERENCIA,
                        new BigDecimal("5000.00"), "Pago al proveedor"));

        assertThat(r.medioPago()).isEqualTo(MedioPago.TRANSFERENCIA);
        assertThat(r.monto()).isEqualByComparingTo("5000.00");
    }

    @Test
    @DisplayName("Egreso con la sesión cerrada: falla")
    void egreso_sesionCerrada_falla() {
        when(sesionCajaRepository.findByIdForUpdate(ID_SESION))
                .thenReturn(Optional.of(sesion(EstadoSesionCaja.CERRADA)));

        assertThatThrownBy(() -> cajaService.registrarEgreso(ID_SESION,
                new MovimientoCajaCreateDTO(ConceptoMovimientoCaja.RETIRO, MedioPago.EFECTIVO,
                        new BigDecimal("10.00"), null)))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("no está abierta");
    }

    @Test
    @DisplayName("Egreso con concepto VENTA: no se permite en este endpoint")
    void egreso_conceptoVenta_falla() {
        when(sesionCajaRepository.findByIdForUpdate(ID_SESION))
                .thenReturn(Optional.of(sesion(EstadoSesionCaja.ABIERTA)));

        assertThatThrownBy(() -> cajaService.registrarEgreso(ID_SESION,
                new MovimientoCajaCreateDTO(ConceptoMovimientoCaja.VENTA, MedioPago.EFECTIVO,
                        new BigDecimal("10.00"), null)))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("RETIRO o GASTO");
    }

    // ============================ CIERRE ============================

    @Test
    @DisplayName("Cierre sin diferencia: queda CERRADA y crea los dos detalles")
    void cerrar_sinDiferencia() {
        prepararCierre(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        when(usuarioService.obtenerAutenticado())
                .thenReturn(usuario(5L, "Ana", Rol.VENDEDOR));
        when(sesionCajaRepository.save(any(SesionCaja.class))).thenAnswer(inv -> inv.getArgument(0));

        SesionCajaResponseDTO r = cajaService.cerrar(ID_SESION,
                new CerrarCajaDTO(new BigDecimal("1000.00"), new BigDecimal("0.00"), null));

        assertThat(r.estado()).isEqualTo(EstadoSesionCaja.CERRADA);
        assertThat(r.totales().totalEsperado()).isEqualByComparingTo("1000.00");
        assertThat(r.totales().totalDeclarado()).isEqualByComparingTo("1000.00");
        assertThat(r.totales().diferencia()).isEqualByComparingTo("0.00");
        assertThat(r.fechaCierre()).isNotNull();
        assertThat(r.horaCierre()).isNotNull();

        ArgumentCaptor<CierreCajaDetalle> captor =
                ArgumentCaptor.forClass(CierreCajaDetalle.class);
        verify(cierreCajaDetalleRepository, times(2)).save(captor.capture());
        List<CierreCajaDetalle> detalles = captor.getAllValues();
        assertThat(detalles.get(0).getId().getMedioPago()).isEqualTo(MedioPago.EFECTIVO);
        assertThat(detalles.get(0).getTotalEsperado()).isEqualByComparingTo("1000.00");
        assertThat(detalles.get(0).getTotalDeclarado()).isEqualByComparingTo("1000.00");
        assertThat(detalles.get(0).getDiferencia()).isEqualByComparingTo("0.00");
        assertThat(detalles.get(1).getId().getMedioPago()).isEqualTo(MedioPago.TRANSFERENCIA);
        assertThat(detalles.get(1).getTotalEsperado()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Cierre con egreso por transferencia: el neto negativo cierra sin diferencia")
    void cerrar_egresoTransferenciaSinDiferencia() {
        prepararCierre(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("500.00"));
        when(usuarioService.obtenerAutenticado())
                .thenReturn(usuario(5L, "Ana", Rol.VENDEDOR));
        when(sesionCajaRepository.save(any(SesionCaja.class))).thenAnswer(inv -> inv.getArgument(0));

        SesionCajaResponseDTO r = cajaService.cerrar(ID_SESION,
                new CerrarCajaDTO(new BigDecimal("1000.00"), new BigDecimal("-500.00"), null));

        assertThat(r.estado()).isEqualTo(EstadoSesionCaja.CERRADA);
        assertThat(r.totales().totalEsperado()).isEqualByComparingTo("500.00");
        assertThat(r.totales().totalDeclarado()).isEqualByComparingTo("500.00");
        assertThat(r.totales().diferencia()).isEqualByComparingTo("0.00");

        ArgumentCaptor<CierreCajaDetalle> captor =
                ArgumentCaptor.forClass(CierreCajaDetalle.class);
        verify(cierreCajaDetalleRepository, times(2)).save(captor.capture());
        CierreCajaDetalle transferencia = captor.getAllValues().stream()
                .filter(d -> d.getId().getMedioPago() == MedioPago.TRANSFERENCIA)
                .findFirst().orElseThrow();
        assertThat(transferencia.getTotalEsperado()).isEqualByComparingTo("-500.00");
        assertThat(transferencia.getTotalDeclarado()).isEqualByComparingTo("-500.00");
        assertThat(transferencia.getDiferencia()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Cierre con neto por transferencia distinto y sin observación: falla")
    void cerrar_diferenciaTransferenciaSinObservacion_falla() {
        prepararCierre(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("500.00"));

        assertThatThrownBy(() -> cajaService.cerrar(ID_SESION,
                new CerrarCajaDTO(new BigDecimal("1000.00"), new BigDecimal("0.00"), null)))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("observación");
        verify(cierreCajaDetalleRepository, never()).save(any());
        verify(sesionCajaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cierre con neto por transferencia distinto y observación: PENDIENTE_REVISION")
    void cerrar_diferenciaTransferenciaConObservacion() {
        prepararCierre(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("500.00"));
        when(usuarioService.obtenerAutenticado())
                .thenReturn(usuario(5L, "Ana", Rol.VENDEDOR));
        when(sesionCajaRepository.save(any(SesionCaja.class))).thenAnswer(inv -> inv.getArgument(0));

        SesionCajaResponseDTO r = cajaService.cerrar(ID_SESION,
                new CerrarCajaDTO(new BigDecimal("1000.00"), new BigDecimal("0.00"),
                        "Se pagó una transferencia de $500"));

        assertThat(r.estado()).isEqualTo(EstadoSesionCaja.PENDIENTE_REVISION);
        assertThat(r.totales().diferencia()).isEqualByComparingTo("500.00");

        ArgumentCaptor<CierreCajaDetalle> captor =
                ArgumentCaptor.forClass(CierreCajaDetalle.class);
        verify(cierreCajaDetalleRepository, times(2)).save(captor.capture());
        CierreCajaDetalle transferencia = captor.getAllValues().stream()
                .filter(d -> d.getId().getMedioPago() == MedioPago.TRANSFERENCIA)
                .findFirst().orElseThrow();
        assertThat(transferencia.getDiferencia()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("Cierre con diferencia sin observación: falla")
    void cerrar_conDiferenciaSinObservacion_falla() {
        prepararCierre(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        assertThatThrownBy(() -> cajaService.cerrar(ID_SESION,
                new CerrarCajaDTO(new BigDecimal("900.00"), new BigDecimal("0.00"), " ")))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("observación");
        verify(cierreCajaDetalleRepository, never()).save(any());
        verify(sesionCajaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cierre con diferencia y observación: queda PENDIENTE_REVISION")
    void cerrar_conDiferenciaConObservacion() {
        prepararCierre(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        when(usuarioService.obtenerAutenticado())
                .thenReturn(usuario(5L, "Ana", Rol.VENDEDOR));
        when(sesionCajaRepository.save(any(SesionCaja.class))).thenAnswer(inv -> inv.getArgument(0));

        SesionCajaResponseDTO r = cajaService.cerrar(ID_SESION,
                new CerrarCajaDTO(new BigDecimal("900.00"), new BigDecimal("0.00"),
                        "Faltaron $100 en efectivo"));

        assertThat(r.estado()).isEqualTo(EstadoSesionCaja.PENDIENTE_REVISION);
        assertThat(r.totales().diferencia()).isEqualByComparingTo("-100.00");
        assertThat(r.observaciones()).isEqualTo("Faltaron $100 en efectivo");
    }

    @Test
    @DisplayName("Cerrar una sesión ya cerrada: falla")
    void cerrar_yaCerrada_falla() {
        when(sesionCajaRepository.findByIdForUpdate(ID_SESION))
                .thenReturn(Optional.of(sesion(EstadoSesionCaja.CERRADA)));

        assertThatThrownBy(() -> cajaService.cerrar(ID_SESION,
                new CerrarCajaDTO(new BigDecimal("1000.00"), new BigDecimal("0.00"), null)))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("ya está cerrada");
    }

    // ============================ REVISIÓN ============================

    @Test
    @DisplayName("Revisar una sesión pendiente: pasa a CERRADA sin cambiar montos")
    void revisar_pendiente() {
        SesionCaja pendiente = sesion(EstadoSesionCaja.PENDIENTE_REVISION);
        pendiente.setTotalEsperado(new BigDecimal("1000.00"));
        pendiente.setTotalDeclarado(new BigDecimal("900.00"));
        pendiente.setDiferencia(new BigDecimal("-100.00"));
        when(sesionCajaRepository.findByIdForUpdate(ID_SESION))
                .thenReturn(Optional.of(pendiente));
        when(usuarioService.obtenerAutenticado())
                .thenReturn(usuario(1L, "Admin", Rol.ADMINISTRADOR));
        when(sesionCajaRepository.save(any(SesionCaja.class))).thenAnswer(inv -> inv.getArgument(0));

        SesionCajaResponseDTO r = cajaService.revisar(ID_SESION,
                new RevisionCajaDTO("Se verificó con el banco"));

        assertThat(r.estado()).isEqualTo(EstadoSesionCaja.CERRADA);
        assertThat(r.usuarioRevision()).isEqualTo("Admin");
        assertThat(r.observacionRevision()).isEqualTo("Se verificó con el banco");
        assertThat(r.fechaRevision()).isNotNull();
        assertThat(r.totales().totalEsperado()).isEqualByComparingTo("1000.00");
        assertThat(r.totales().totalDeclarado()).isEqualByComparingTo("900.00");
        assertThat(r.totales().diferencia()).isEqualByComparingTo("-100.00");
    }

    @Test
    @DisplayName("Revisar una sesión que no está pendiente: falla")
    void revisar_noPendiente_falla() {
        when(sesionCajaRepository.findByIdForUpdate(ID_SESION))
                .thenReturn(Optional.of(sesion(EstadoSesionCaja.CERRADA)));

        assertThatThrownBy(() -> cajaService.revisar(ID_SESION,
                new RevisionCajaDTO("Observación")))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("no está pendiente");
    }

    // ============================ CONSULTAS ============================

    @Test
    @DisplayName("Listar pasa los filtros al repositorio")
    void listar_filtros() {
        when(sesionCajaRepository.buscar(eq(EstadoSesionCaja.CERRADA), eq(5L), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        Page<SesionCajaResponseDTO> r = cajaService.listar(
                EstadoSesionCaja.CERRADA, 5L,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                PageRequest.of(0, 10));

        assertThat(r).isEmpty();
        verify(sesionCajaRepository).buscar(
                eq(EstadoSesionCaja.CERRADA), eq(5L), any(), any(), any());
    }

    @Test
    @DisplayName("Obtener el detalle de una sesión inexistente: 404")
    void obtenerDetalle_inexistente() {
        when(sesionCajaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cajaService.obtenerDetalle(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("99");
    }

    // ======================= ANULACIÓN DE VENTAS (CAJA-07) =======================

    private MovimientoCaja ingresoDeVenta(MedioPago medio, String monto) {
        MovimientoCaja m = new MovimientoCaja();
        m.setId_venta(3L);
        m.setMedioPago(medio);
        m.setMonto(new BigDecimal(monto));
        return m;
    }

    private void prepararSesionAbiertaParaAnulacion() {
        SesionCaja abierta = sesion(EstadoSesionCaja.ABIERTA);
        abierta.setMontoInicial(BigDecimal.ZERO);
        when(sesionCajaRepository.findFirstByEstadoOrderByFechaAperturaDescHoraAperturaDesc(
                EstadoSesionCaja.ABIERTA)).thenReturn(Optional.of(abierta));
        when(sesionCajaRepository.findByIdForUpdate(ID_SESION)).thenReturn(Optional.of(abierta));
    }

    private void prepararEfectivoDisponible(String ingresos) {
        when(movimientoCajaRepository.sumarPorTipoYMedio(
                ID_SESION, MedioPago.EFECTIVO, TipoMovimientoCaja.INGRESO))
                .thenReturn(new BigDecimal(ingresos));
        when(movimientoCajaRepository.sumarPorTipoYMedio(
                ID_SESION, MedioPago.EFECTIVO, TipoMovimientoCaja.EGRESO))
                .thenReturn(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Anular un pago mixto: un EGRESO/ANULACION por pago, con motivo y en la sesión actual")
    void anularCobros_pagoMixto_generaContramovimientos() {
        prepararSesionAbiertaParaAnulacion();
        prepararEfectivoDisponible("1500.00");
        when(movimientoCajaRepository.listarPorVentaYTipo(3L, TipoMovimientoCaja.INGRESO))
                .thenReturn(List.of(
                        ingresoDeVenta(MedioPago.EFECTIVO, "100.00"),
                        ingresoDeVenta(MedioPago.TRANSFERENCIA, "150.00")));
        when(usuarioService.obtenerAutenticado())
                .thenReturn(usuario(1L, "Ana", Rol.VENDEDOR));

        cajaService.anularCobrosDeVenta(3L, "  Producto defectuoso  ");

        ArgumentCaptor<MovimientoCaja> captor = ArgumentCaptor.forClass(MovimientoCaja.class);
        verify(movimientoCajaRepository, times(2)).save(captor.capture());
        List<MovimientoCaja> egresos = captor.getAllValues();
        assertThat(egresos).extracting(MovimientoCaja::getTipo)
                .containsOnly(TipoMovimientoCaja.EGRESO);
        assertThat(egresos).extracting(MovimientoCaja::getConcepto)
                .containsOnly(ConceptoMovimientoCaja.ANULACION);
        assertThat(egresos).extracting(MovimientoCaja::getId_venta).containsOnly(3L);
        assertThat(egresos).extracting(m -> m.getSesionCaja().getId_sesion())
                .containsOnly(ID_SESION);
        assertThat(egresos).extracting(m -> m.getUsuario().getId_usuario()).containsOnly(1L);
        assertThat(egresos).extracting(MovimientoCaja::getMedioPago)
                .containsExactly(MedioPago.EFECTIVO, MedioPago.TRANSFERENCIA);
        assertThat(egresos).extracting(MovimientoCaja::getMonto)
                .containsExactly(new BigDecimal("100.00"), new BigDecimal("150.00"));
        assertThat(egresos).extracting(MovimientoCaja::getObservaciones)
                .containsExactly("Anulación de la venta #3: Producto defectuoso",
                        "Anulación de la venta #3: Producto defectuoso");
        assertThat(egresos).allSatisfy(m -> {
            assertThat(m.getFecha()).isNotNull();
            assertThat(m.getHora()).isNotNull();
        });
    }

    @Test
    @DisplayName("Anular sin caja abierta: falla y no guarda contramovimientos")
    void anularCobros_sinCajaAbierta_falla() {
        when(sesionCajaRepository.findFirstByEstadoOrderByFechaAperturaDescHoraAperturaDesc(
                EstadoSesionCaja.ABIERTA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cajaService.anularCobrosDeVenta(3L, "Motivo"))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("caja abierta");
        verify(movimientoCajaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Anular con efectivo insuficiente en la sesión actual: falla y no guarda nada")
    void anularCobros_efectivoInsuficiente_falla() {
        prepararSesionAbiertaParaAnulacion();
        prepararEfectivoDisponible("0.00");
        when(movimientoCajaRepository.listarPorVentaYTipo(3L, TipoMovimientoCaja.INGRESO))
                .thenReturn(List.of(ingresoDeVenta(MedioPago.EFECTIVO, "1000.01")));

        assertThatThrownBy(() -> cajaService.anularCobrosDeVenta(3L, "Motivo"))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("efectivo suficiente");
        verify(movimientoCajaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Anular una venta pagada con transferencia: no hay límite de efectivo")
    void anularCobros_transferenciaSinLimite() {
        prepararSesionAbiertaParaAnulacion();
        when(movimientoCajaRepository.listarPorVentaYTipo(3L, TipoMovimientoCaja.INGRESO))
                .thenReturn(List.of(ingresoDeVenta(MedioPago.TRANSFERENCIA, "5000.00")));
        when(usuarioService.obtenerAutenticado())
                .thenReturn(usuario(1L, "Ana", Rol.VENDEDOR));

        cajaService.anularCobrosDeVenta(3L, "Motivo");

        ArgumentCaptor<MovimientoCaja> captor = ArgumentCaptor.forClass(MovimientoCaja.class);
        verify(movimientoCajaRepository).save(captor.capture());
        assertThat(captor.getValue().getMonto()).isEqualByComparingTo("5000.00");
        assertThat(captor.getValue().getMedioPago()).isEqualTo(MedioPago.TRANSFERENCIA);
    }
}

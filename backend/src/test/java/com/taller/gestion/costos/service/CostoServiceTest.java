package com.taller.gestion.costos.service;

import com.taller.gestion.caja.service.CajaService;
import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.common.exception.OperacionNoPermitidaException;
import com.taller.gestion.common.exception.RecursoNoEncontradoException;
import com.taller.gestion.costos.dto.CostoAnularDTO;
import com.taller.gestion.costos.dto.CostoCreateDTO;
import com.taller.gestion.costos.dto.CostoResponseDTO;
import com.taller.gestion.costos.dto.CostoUpdateDTO;
import com.taller.gestion.costos.entity.Costo;
import com.taller.gestion.costos.entity.TipoCosto;
import com.taller.gestion.costos.repository.CostoRepository;
import com.taller.gestion.costos.repository.TipoCostoRepository;
import com.taller.gestion.usuario.entity.Usuario;
import com.taller.gestion.usuario.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CostoServiceTest {

    @Mock
    private CostoRepository costoRepository;

    @Mock
    private TipoCostoRepository tipoCostoRepository;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private CajaService cajaService;

    @InjectMocks
    private CostoService costoService;

    private TipoCosto tipoHabilitado() {
        TipoCosto tipo = new TipoCosto();
        tipo.setId_tipo_costo(1L);
        tipo.setNombre("Alquiler");
        tipo.setActivo(true);
        tipo.setReservado(false);
        return tipo;
    }

    private Usuario usuarioAutenticado() {
        Usuario usuario = new Usuario();
        usuario.setId_usuario(7L);
        usuario.setEmail("operador@taller.local");
        usuario.setActivo(true);
        return usuario;
    }

    private Costo costoBase() {
        Costo costo = new Costo();
        costo.setId_costo(10L);
        costo.setTipoCosto(tipoHabilitado());
        costo.setDescripcion("Alquiler local");
        costo.setMonto(new BigDecimal("150.00"));
        costo.setFechaPago(LocalDate.of(2026, 11, 19));
        costo.setPeriodoMes(11);
        costo.setPeriodoAnio(2026);
        costo.setObservaciones("Pago mensual");
        costo.setUsuario(usuarioAutenticado());
        return costo;
    }

    private CostoCreateDTO dtoAlta() {
        return new CostoCreateDTO(1L, "Alquiler local", new BigDecimal("150.00"),
                LocalDate.of(2026, 11, 19), 11, 2026, null);
    }

    private CostoUpdateDTO dtoEdicion() {
        return new CostoUpdateDTO(1L, "Alquiler local", new BigDecimal("175.00"),
                LocalDate.of(2026, 11, 20), 11, 2026, "Ajuste");
    }

    // ============================ CONSULTAS ============================

    @Test
    @DisplayName("Buscar: mapea la respuesta y marca los costos generados por un ingreso")
    void buscar_mapeaRespuesta() {
        Costo costo = costoBase();
        costo.setId_ingreso_mercaderia(99L);
        Pageable pagina = PageRequest.of(0, 20);
        when(costoRepository.buscar(null, 11, 2026, false, pagina))
                .thenReturn(new PageImpl<>(List.of(costo), pagina, 1));

        var resultado = costoService.buscar(null, 11, 2026, false, pagina);

        CostoResponseDTO dto = resultado.getContent().get(0);
        assertThat(dto.id_costo()).isEqualTo(10L);
        assertThat(dto.nombreTipoCosto()).isEqualTo("Alquiler");
        assertThat(dto.id_usuario()).isEqualTo(7L);
        assertThat(dto.id_ingreso_mercaderia()).isEqualTo(99L);
        assertThat(dto.generadoPorIngreso()).isTrue();
        assertThat(dto.anulado()).isFalse();
    }

    @Test
    @DisplayName("Total: devuelve la suma del repositorio")
    void total_devuelveSuma() {
        when(costoRepository.total(null, 11, 2026)).thenReturn(new BigDecimal("300.00"));

        assertThat(costoService.total(null, 11, 2026)).isEqualByComparingTo("300.00");
    }

    @Test
    @DisplayName("FindById: encuentra un costo y lo mapea")
    void findById_existente() {
        when(costoRepository.findById(10L)).thenReturn(Optional.of(costoBase()));

        assertThat(costoService.findById(10L).id_costo()).isEqualTo(10L);
    }

    @Test
    @DisplayName("FindById: lanza 404 si no existe")
    void findById_inexistente() {
        when(costoRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> costoService.findById(10L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // ============================ ALTA ============================

    @Test
    @DisplayName("Alta: guarda el costo con la entidad del usuario autenticado")
    void crear_asociaUsuarioAutenticado() {
        Usuario usuario = usuarioAutenticado();
        when(tipoCostoRepository.findById(1L)).thenReturn(Optional.of(tipoHabilitado()));
        when(usuarioService.obtenerAutenticado()).thenReturn(usuario);
        when(costoRepository.save(any(Costo.class))).thenAnswer(inv -> inv.getArgument(0));

        CostoResponseDTO resultado = costoService.crear(dtoAlta());

        ArgumentCaptor<Costo> captor = ArgumentCaptor.forClass(Costo.class);
        verify(costoRepository).save(captor.capture());
        assertThat(captor.getValue().getUsuario()).isSameAs(usuario);
        assertThat(resultado.id_usuario()).isEqualTo(7L);
    }

    @Test
    @DisplayName("Alta: sin sesión no guarda y propaga el error 401")
    void crear_sinSesionNoGuarda() {
        when(tipoCostoRepository.findById(1L)).thenReturn(Optional.of(tipoHabilitado()));
        when(usuarioService.obtenerAutenticado()).thenThrow(
                new AuthenticationCredentialsNotFoundException("Sin sesión"));

        assertThatThrownBy(() -> costoService.crear(dtoAlta()))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        verify(costoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Alta: rechaza un tipo inexistente antes de pedir el usuario")
    void crear_tipoInexistente() {
        when(tipoCostoRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> costoService.crear(dtoAlta()))
                .isInstanceOf(DatosInvalidosException.class);
        verifyNoInteractions(usuarioService);
        verify(costoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Alta: rechaza el tipo reservado de insumos")
    void crear_tipoReservado() {
        TipoCosto tipo = tipoHabilitado();
        tipo.setReservado(true);
        when(tipoCostoRepository.findById(1L)).thenReturn(Optional.of(tipo));

        assertThatThrownBy(() -> costoService.crear(dtoAlta()))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("ingresar mercadería");
        verifyNoInteractions(usuarioService);
    }

    @Test
    @DisplayName("Alta: rechaza un tipo desactivado")
    void crear_tipoInactivo() {
        TipoCosto tipo = tipoHabilitado();
        tipo.setActivo(false);
        when(tipoCostoRepository.findById(1L)).thenReturn(Optional.of(tipo));

        assertThatThrownBy(() -> costoService.crear(dtoAlta()))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("desactivado");
        verifyNoInteractions(usuarioService);
    }

    // ============================ EDICIÓN ============================

    @Test
    @DisplayName("Edición: actualiza los datos pero conserva al usuario que creó el costo")
    void actualizar_conservaUsuarioDeAlta() {
        Costo costo = costoBase();
        Usuario creador = costo.getUsuario();
        when(costoRepository.findById(10L)).thenReturn(Optional.of(costo));
        when(tipoCostoRepository.findById(1L)).thenReturn(Optional.of(tipoHabilitado()));

        CostoResponseDTO resultado = costoService.actualizar(10L, dtoEdicion());

        assertThat(costo.getMonto()).isEqualByComparingTo("175.00");
        assertThat(costo.getDescripcion()).isEqualTo("Alquiler local");
        assertThat(costo.getObservaciones()).isEqualTo("Ajuste");
        assertThat(costo.getUsuario()).isSameAs(creador);
        assertThat(resultado.id_usuario()).isEqualTo(7L);
        verifyNoInteractions(usuarioService);
    }

    @Test
    @DisplayName("Edición: rechaza un costo anulado")
    void actualizar_costoAnulado() {
        Costo costo = costoBase();
        costo.setAnulado(true);
        when(costoRepository.findById(10L)).thenReturn(Optional.of(costo));

        assertThatThrownBy(() -> costoService.actualizar(10L, dtoEdicion()))
                .isInstanceOf(OperacionNoPermitidaException.class);
        verifyNoInteractions(tipoCostoRepository, usuarioService);
    }

    @Test
    @DisplayName("Edición: rechaza un costo generado por un ingreso de mercadería")
    void actualizar_costoGeneradoPorIngreso() {
        Costo costo = costoBase();
        costo.setId_ingreso_mercaderia(99L);
        when(costoRepository.findById(10L)).thenReturn(Optional.of(costo));

        assertThatThrownBy(() -> costoService.actualizar(10L, dtoEdicion()))
                .isInstanceOf(OperacionNoPermitidaException.class);
        verifyNoInteractions(tipoCostoRepository, usuarioService);
    }

    // ============================ ANULACIÓN ============================

    @Test
    @DisplayName("Anulación: registra al usuario autenticado como responsable")
    void anular_registraUsuarioDeAnulacion() {
        Usuario usuario = usuarioAutenticado();
        Costo costo = costoBase();
        when(costoRepository.findById(10L)).thenReturn(Optional.of(costo));
        when(usuarioService.obtenerAutenticado()).thenReturn(usuario);

        CostoResponseDTO resultado = costoService.anular(10L, new CostoAnularDTO("Carga duplicada"));

        assertThat(costo.getUsuarioAnulacion()).isSameAs(usuario);
        assertThat(costo.isAnulado()).isTrue();
        assertThat(costo.getFechaAnulacion()).isNotNull();
        assertThat(resultado.motivoAnulacion()).isEqualTo("Carga duplicada");
        assertThat(resultado.id_usuario_anulacion()).isEqualTo(7L);
        verify(cajaService).existeEgresoVinculadoACosto(10L);
    }

    @Test
    @DisplayName("Anulación: rechaza un costo vinculado a un egreso de caja (CST-05)")
    void anular_conEgresoVinculado() {
        Costo costo = costoBase();
        when(costoRepository.findById(10L)).thenReturn(Optional.of(costo));
        when(cajaService.existeEgresoVinculadoACosto(10L)).thenReturn(true);

        assertThatThrownBy(() -> costoService.anular(10L, new CostoAnularDTO("Motivo")))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("anulá primero el egreso");

        assertThat(costo.isAnulado()).isFalse();
        verifyNoInteractions(usuarioService);
    }

    @Test
    @DisplayName("Anulación: rechaza un costo ya anulado")
    void anular_costoYaAnulado() {
        Costo costo = costoBase();
        costo.setAnulado(true);
        when(costoRepository.findById(10L)).thenReturn(Optional.of(costo));

        assertThatThrownBy(() -> costoService.anular(10L, new CostoAnularDTO("Motivo")))
                .isInstanceOf(OperacionNoPermitidaException.class);
        verifyNoInteractions(usuarioService);
    }

    @Test
    @DisplayName("Anulación: rechaza un costo generado por un ingreso de mercadería")
    void anular_costoGeneradoPorIngreso() {
        Costo costo = costoBase();
        costo.setId_ingreso_mercaderia(99L);
        when(costoRepository.findById(10L)).thenReturn(Optional.of(costo));

        assertThatThrownBy(() -> costoService.anular(10L, new CostoAnularDTO("Motivo")))
                .isInstanceOf(OperacionNoPermitidaException.class);
        verifyNoInteractions(usuarioService);
    }

    @Test
    @DisplayName("Anulación: rechaza un inexistente")
    void anular_inexistente() {
        when(costoRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> costoService.anular(10L, new CostoAnularDTO("Motivo")))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(usuarioService);
    }
}

package com.taller.gestion.caja.controller;

import com.taller.gestion.caja.dto.AbrirCajaDTO;
import com.taller.gestion.caja.dto.CajaActualDTO;
import com.taller.gestion.caja.dto.CerrarCajaDTO;
import com.taller.gestion.caja.dto.MovimientoCajaCreateDTO;
import com.taller.gestion.caja.dto.MovimientoCajaResponseDTO;
import com.taller.gestion.caja.dto.RevisionCajaDTO;
import com.taller.gestion.caja.dto.SesionCajaDetalleDTO;
import com.taller.gestion.caja.dto.SesionCajaResponseDTO;
import com.taller.gestion.caja.dto.TotalesCajaDTO;
import com.taller.gestion.caja.entity.ConceptoMovimientoCaja;
import com.taller.gestion.caja.entity.EstadoSesionCaja;
import com.taller.gestion.caja.entity.MedioPago;
import com.taller.gestion.caja.entity.TipoMovimientoCaja;
import com.taller.gestion.caja.service.CajaService;
import com.taller.gestion.common.security.ErrorResponseWriter;
import com.taller.gestion.common.security.RestAccessDeniedHandler;
import com.taller.gestion.common.security.RestAuthenticationEntryPoint;
import com.taller.gestion.common.security.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CajaController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class, ErrorResponseWriter.class})
@WithMockUser(roles = "VENDEDOR")
class CajaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CajaService cajaService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private TotalesCajaDTO totales(EstadoSesionCaja estado) {
        boolean cerrada = estado != EstadoSesionCaja.ABIERTA;
        return new TotalesCajaDTO(
                new BigDecimal("1000.00"),
                new BigDecimal("0.00"),
                cerrada ? new BigDecimal("1000.00") : null,
                cerrada ? new BigDecimal("0.00") : null,
                cerrada ? new BigDecimal("0.00") : null,
                cerrada ? new BigDecimal("0.00") : null,
                new BigDecimal("1000.00"),
                cerrada ? new BigDecimal("1000.00") : null,
                cerrada ? new BigDecimal("0.00") : null);
    }

    private SesionCajaResponseDTO sesion(EstadoSesionCaja estado) {
        boolean abierta = estado == EstadoSesionCaja.ABIERTA;
        return new SesionCajaResponseDTO(
                1L,
                5L,
                "Ana",
                LocalDate.of(2026, 11, 20),
                LocalTime.of(9, 0),
                abierta ? null : 6L,
                abierta ? null : "Beto",
                abierta ? null : LocalDate.of(2026, 11, 20),
                abierta ? null : LocalTime.of(18, 0),
                new BigDecimal("1000.00"),
                null,
                estado,
                totales(estado),
                null,
                null,
                null,
                null,
                null);
    }

    private MovimientoCajaResponseDTO movimiento() {
        return new MovimientoCajaResponseDTO(
                10L, 1L, 5L, "Ana", null, null,
                TipoMovimientoCaja.EGRESO, ConceptoMovimientoCaja.RETIRO,
                new BigDecimal("200.00"), MedioPago.EFECTIVO,
                LocalDate.of(2026, 11, 20), LocalTime.of(10, 30), "Retiro para depositar");
    }

    // ============================ CAJA ACTUAL ============================

    @Test
    @DisplayName("GET /api/caja/actual: devuelve la sesión abierta y la sugerencia")
    void actual_conSesionAbierta() throws Exception {
        when(cajaService.actual()).thenReturn(new CajaActualDTO(
                sesion(EstadoSesionCaja.ABIERTA),
                new BigDecimal("1000.00"),
                List.of(movimiento())));

        mockMvc.perform(get("/api/caja/actual"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sesion.id_sesion").value(1))
                .andExpect(jsonPath("$.sesion.estado").value("ABIERTA"))
                .andExpect(jsonPath("$.montoInicialSugerido").value(1000.00))
                .andExpect(jsonPath("$.movimientos[0].concepto").value("RETIRO"));
    }

    @Test
    @DisplayName("GET /api/caja/actual sin sesión: sesión nula y movimientos vacíos")
    void actual_sinSesion() throws Exception {
        when(cajaService.actual()).thenReturn(
                new CajaActualDTO(null, BigDecimal.ZERO, List.of()));

        mockMvc.perform(get("/api/caja/actual"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sesion").doesNotExist())
                .andExpect(jsonPath("$.montoInicialSugerido").value(0))
                .andExpect(jsonPath("$.movimientos").isEmpty());
    }

    @Test
    @DisplayName("GET /api/caja/actual sin autenticación: 401")
    @WithAnonymousUser
    void actual_sinAutenticacion_devuelve401() throws Exception {
        mockMvc.perform(get("/api/caja/actual"))
                .andExpect(status().isUnauthorized());
    }

    // ============================ APERTURA ============================

    @Test
    @DisplayName("POST /api/caja/sesiones: apertura válida responde 201")
    void abrir_valida() throws Exception {
        when(cajaService.abrir(any(AbrirCajaDTO.class)))
                .thenReturn(sesion(EstadoSesionCaja.ABIERTA));

        mockMvc.perform(post("/api/caja/sesiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AbrirCajaDTO(new BigDecimal("1000.00"), null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id_sesion").value(1))
                .andExpect(jsonPath("$.estado").value("ABIERTA"));
    }

    @Test
    @DisplayName("POST /api/caja/sesiones sin monto: 400 por validación")
    void abrir_sinMonto_devuelve400() throws Exception {
        mockMvc.perform(post("/api/caja/sesiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivoAjusteInicial\":\"sin monto\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").isArray());

        verifyNoInteractions(cajaService);
    }

    // ============================ EGRESOS ============================

    @Test
    @DisplayName("POST /api/caja/sesiones/{id}/movimientos: egreso válido responde 201")
    void registrarEgreso_valido() throws Exception {
        when(cajaService.registrarEgreso(eq(1L), any(MovimientoCajaCreateDTO.class)))
                .thenReturn(movimiento());

        mockMvc.perform(post("/api/caja/sesiones/1/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MovimientoCajaCreateDTO(
                                ConceptoMovimientoCaja.RETIRO,
                                MedioPago.EFECTIVO,
                                new BigDecimal("200.00"),
                                "Retiro para depositar", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id_movimiento_caja").value(10))
                .andExpect(jsonPath("$.tipo").value("EGRESO"))
                .andExpect(jsonPath("$.monto").value(200.00));
    }

    @Test
    @DisplayName("POST egreso con id_costo: el vínculo llega al service")
    void registrarEgreso_conCostoVinculado_pasaIdCosto() throws Exception {
        when(cajaService.registrarEgreso(eq(1L), any(MovimientoCajaCreateDTO.class)))
                .thenReturn(movimiento());

        mockMvc.perform(post("/api/caja/sesiones/1/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"concepto":"GASTO","medioPago":"TRANSFERENCIA",
                                 "monto":200.00,"observaciones":"Pago de alquiler","id_costo":10}
                                """))
                .andExpect(status().isCreated());

        ArgumentCaptor<MovimientoCajaCreateDTO> captor =
                ArgumentCaptor.forClass(MovimientoCajaCreateDTO.class);
        verify(cajaService).registrarEgreso(eq(1L), captor.capture());
        assertThat(captor.getValue().id_costo()).isEqualTo(10L);
    }

    @Test
    @DisplayName("POST egresos con monto cero: 400 por validación")
    void registrarEgreso_montoCero_devuelve400() throws Exception {
        mockMvc.perform(post("/api/caja/sesiones/1/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MovimientoCajaCreateDTO(
                                ConceptoMovimientoCaja.GASTO,
                                MedioPago.EFECTIVO,
                                BigDecimal.ZERO,
                                null, null))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cajaService);
    }

    // ============================ CIERRE ============================

    @Test
    @DisplayName("POST /api/caja/sesiones/{id}/cierre: cierre válido responde 200")
    void cerrar_valido() throws Exception {
        when(cajaService.cerrar(eq(1L), any(CerrarCajaDTO.class)))
                .thenReturn(sesion(EstadoSesionCaja.CERRADA));

        mockMvc.perform(post("/api/caja/sesiones/1/cierre")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CerrarCajaDTO(
                                new BigDecimal("1000.00"),
                                new BigDecimal("0.00"),
                                null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADA"))
                .andExpect(jsonPath("$.totales.diferencia").value(0.00));
    }

    @Test
    @DisplayName("POST cierre con neto por transferencia negativo: 200")
    void cerrar_transferenciaNegativa_valido() throws Exception {
        when(cajaService.cerrar(eq(1L), any(CerrarCajaDTO.class)))
                .thenReturn(sesion(EstadoSesionCaja.CERRADA));

        mockMvc.perform(post("/api/caja/sesiones/1/cierre")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CerrarCajaDTO(
                                new BigDecimal("1000.00"),
                                new BigDecimal("-500.00"),
                                null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADA"));

        verify(cajaService).cerrar(eq(1L), any(CerrarCajaDTO.class));
    }

    @Test
    @DisplayName("POST cierre con efectivo negativo: 400 por validación")
    void cerrar_efectivoNegativo_devuelve400() throws Exception {
        mockMvc.perform(post("/api/caja/sesiones/1/cierre")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CerrarCajaDTO(
                                new BigDecimal("-1.00"),
                                new BigDecimal("0.00"),
                                "Efectivo negativo"))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cajaService);
    }

    // ============================ REVISIÓN ============================

    @Test
    @DisplayName("POST revisión con vendedor: 403")
    void revisar_vendedor_devuelve403() throws Exception {
        mockMvc.perform(post("/api/caja/sesiones/1/revision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RevisionCajaDTO("Se verificó"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(cajaService);
    }

    @Test
    @DisplayName("POST revisión con administrador: 200 y pasa a CERRADA")
    @WithMockUser(roles = "ADMINISTRADOR")
    void revisar_admin() throws Exception {
        SesionCajaResponseDTO revisada = sesion(EstadoSesionCaja.CERRADA);
        when(cajaService.revisar(eq(1L), any(RevisionCajaDTO.class))).thenReturn(revisada);

        mockMvc.perform(post("/api/caja/sesiones/1/revision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RevisionCajaDTO("Se verificó con el banco"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADA"));
    }

    // ============================ CONSULTAS ============================

    @Test
    @DisplayName("GET /api/caja/sesiones: historial paginado")
    void listar() throws Exception {
        when(cajaService.listar(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sesion(EstadoSesionCaja.CERRADA))));

        mockMvc.perform(get("/api/caja/sesiones")
                        .param("estado", "CERRADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id_sesion").value(1))
                .andExpect(jsonPath("$.content[0].estado").value("CERRADA"));
    }

    @Test
    @DisplayName("GET /api/caja/sesiones/{id}: detalle con movimientos")
    void obtener() throws Exception {
        when(cajaService.obtenerDetalle(1L)).thenReturn(
                new SesionCajaDetalleDTO(sesion(EstadoSesionCaja.ABIERTA),
                        List.of(movimiento())));

        mockMvc.perform(get("/api/caja/sesiones/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sesion.id_sesion").value(1))
                .andExpect(jsonPath("$.movimientos[0].id_movimiento_caja").value(10));
    }
}

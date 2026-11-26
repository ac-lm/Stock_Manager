package com.taller.gestion.reportes.controller;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.common.security.ErrorResponseWriter;
import com.taller.gestion.common.security.RestAccessDeniedHandler;
import com.taller.gestion.common.security.RestAuthenticationEntryPoint;
import com.taller.gestion.common.security.SecurityConfig;
import com.taller.gestion.reportes.exportacion.ArchivoExportadoDTO;
import com.taller.gestion.reportes.exportacion.ExportadorReportes;
import com.taller.gestion.reportes.exportacion.FormatoExportacion;
import com.taller.gestion.reportes.exportacion.SolicitudExportacionDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReporteExportacionController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class, ErrorResponseWriter.class})
@WithMockUser(roles = "ADMINISTRADOR")
class ReporteExportacionControllerTest {

    private static final String URL = "/api/reportes/exportar";
    private static final String NOMBRE =
            "ventas-por-periodo_2026-10-01_2026-10-31";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExportadorReportes exportadorReportes;

    @MockitoBean
    private UserDetailsService userDetailsService;

    // ============================ SEGURIDAD ============================

    @Test
    @WithAnonymousUser
    @DisplayName("Sin sesión: la exportación devuelve 401")
    void sinSesion_devuelve401() throws Exception {
        mockMvc.perform(get(URL)
                        .param("reporte", "ventas-por-periodo").param("formato", "csv")
                        .param("desde", "2026-10-01").param("hasta", "2026-10-31"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        verify(exportadorReportes, never()).exportar(any());
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    @DisplayName("Rol vendedor: la exportación devuelve 403")
    void conRolVendedor_devuelve403() throws Exception {
        mockMvc.perform(get(URL)
                        .param("reporte", "ventas-por-periodo").param("formato", "csv")
                        .param("desde", "2026-10-01").param("hasta", "2026-10-31"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verify(exportadorReportes, never()).exportar(any());
    }

    // ============================ DESCARGA ============================

    @ParameterizedTest(name = "formato {0}")
    @CsvSource({
            "xlsx, application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "csv, text/csv;charset=UTF-8",
            "pdf, application/pdf"
    })
    @DisplayName("Administrador: descarga el archivo con content-type y nombre correctos")
    void exportaEnCadaFormato(String formato, String contentType) throws Exception {
        byte[] contenido = "contenido-de-prueba".getBytes(StandardCharsets.UTF_8);
        when(exportadorReportes.exportar(any())).thenReturn(
                new ArchivoExportadoDTO(contenido, contentType, NOMBRE + "." + formato));

        mockMvc.perform(get(URL)
                        .param("reporte", "ventas-por-periodo").param("formato", formato)
                        .param("desde", "2026-10-01").param("hasta", "2026-10-31")
                        .param("agrupacion", "mes"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, contentType))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + NOMBRE + "." + formato + "\""))
                .andExpect(content().bytes(contenido));

        ArgumentCaptor<SolicitudExportacionDTO> captor =
                ArgumentCaptor.forClass(SolicitudExportacionDTO.class);
        verify(exportadorReportes).exportar(captor.capture());
        SolicitudExportacionDTO solicitud = captor.getValue();
        assertThat(solicitud.reporte()).isEqualTo("ventas-por-periodo");
        assertThat(solicitud.formato())
                .isEqualTo(FormatoExportacion.valueOf(formato.toUpperCase()));
        assertThat(solicitud.parametros())
                .containsEntry("desde", "2026-10-01")
                .containsEntry("hasta", "2026-10-31")
                .containsEntry("agrupacion", "mes");
    }

    // ============================ VALIDACIÓN ============================

    @Test
    @DisplayName("Formato desconocido: 400 y no se genera ningún archivo")
    void formatoInvalido_devuelve400() throws Exception {
        mockMvc.perform(get(URL)
                        .param("reporte", "ventas-por-periodo").param("formato", "docx")
                        .param("desde", "2026-10-01").param("hasta", "2026-10-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje")
                        .value("El formato debe ser xlsx, csv o pdf"));
        verify(exportadorReportes, never()).exportar(any());
    }

    @Test
    @DisplayName("Reporte desconocido: 400 con el mensaje del registro")
    void reporteInvalido_devuelve400() throws Exception {
        when(exportadorReportes.exportar(any()))
                .thenThrow(new DatosInvalidosException("El reporte 'otro' no existe"));

        mockMvc.perform(get(URL)
                        .param("reporte", "otro").param("formato", "pdf")
                        .param("desde", "2026-10-01").param("hasta", "2026-10-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El reporte 'otro' no existe"));
    }
}

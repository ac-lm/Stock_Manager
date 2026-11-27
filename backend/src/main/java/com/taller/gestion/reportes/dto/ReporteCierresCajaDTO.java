package com.taller.gestion.reportes.dto;

import com.taller.gestion.caja.entity.EstadoSesionCaja;
import com.taller.gestion.reportes.exportacion.ColumnaExportable;
import com.taller.gestion.reportes.exportacion.ReporteExportable;
import com.taller.gestion.reportes.exportacion.TipoDatoExportable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Schema(description = "Cierres de caja del rango, con esperado, declarado y diferencia por medio "
        + "de pago, e ingresos, egresos, faltantes y sobrantes del período")
public record ReporteCierresCajaDTO(
        @Schema(description = "Primer día del rango de apertura, incluido")
        LocalDate desde,

        @Schema(description = "Último día del rango de apertura, incluido")
        LocalDate hasta,

        @Schema(description = "Usuario que cerró, o null si se muestran todos", example = "7")
        Long id_usuario_cierre,

        @Schema(description = "TODOS, CERRADA o PENDIENTE_REVISION")
        String estado,

        @Schema(description = "TODOS, EFECTIVO o TRANSFERENCIA")
        String medio_pago,

        @Schema(description = "Medios incluidos en columnas y totales", example = "[\"EFECTIVO\"]")
        List<String> medios,

        @Schema(description = "Una fila por sesión cerrada o pendiente de revisión")
        List<CierreCajaFilaDTO> filas,

        @Schema(description = "Totales del período por medio de pago")
        TotalesCierresCajaDTO totales
) implements ReporteExportable {

    private static final DateTimeFormatter FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-AR"));
    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("es-AR"));
    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2);

    @Override
    public String id() {
        return "cierres-caja";
    }

    @Override
    public String titulo() {
        return "Reporte de cierres de caja";
    }

    @Override
    public String descripcionFiltros() {
        List<String> partes = new ArrayList<>();
        partes.add("Estado: " + etiquetaEstado(estado));
        partes.add("Medio de pago: " + etiquetaMedio(medio_pago));
        if (id_usuario_cierre != null) {
            partes.add("Usuario que cerró: " + id_usuario_cierre);
        }
        return String.join("; ", partes);
    }

    @Override
    public List<ColumnaExportable> columnas() {
        List<ColumnaExportable> columnas = new ArrayList<>();
        columnas.add(new ColumnaExportable("Sesión", TipoDatoExportable.TEXTO));
        columnas.add(new ColumnaExportable("Apertura", TipoDatoExportable.TEXTO));
        columnas.add(new ColumnaExportable("Cierre", TipoDatoExportable.TEXTO));
        columnas.add(new ColumnaExportable("Abrió", TipoDatoExportable.TEXTO));
        columnas.add(new ColumnaExportable("Cerró", TipoDatoExportable.TEXTO));
        columnas.add(new ColumnaExportable("Monto inicial", TipoDatoExportable.DECIMAL));
        columnas.add(new ColumnaExportable("Estado", TipoDatoExportable.TEXTO));
        for (String medio : medios) {
            columnas.add(new ColumnaExportable("Esperado " + medio, TipoDatoExportable.DECIMAL));
            columnas.add(new ColumnaExportable("Declarado " + medio, TipoDatoExportable.DECIMAL));
            columnas.add(new ColumnaExportable("Diferencia " + medio, TipoDatoExportable.DECIMAL));
            columnas.add(new ColumnaExportable("Ingresos " + medio, TipoDatoExportable.DECIMAL));
            columnas.add(new ColumnaExportable("Egresos " + medio, TipoDatoExportable.DECIMAL));
            columnas.add(new ColumnaExportable("Faltante " + medio, TipoDatoExportable.DECIMAL));
            columnas.add(new ColumnaExportable("Sobrante " + medio, TipoDatoExportable.DECIMAL));
        }
        return List.copyOf(columnas);
    }

    @Override
    public List<List<Object>> filasExportables() {
        return filas.stream()
                .map(fila -> {
                    List<Object> valores = new ArrayList<>();
                    valores.add(String.valueOf(fila.id_sesion()));
                    valores.add(formatear(fila.fechaApertura(), fila.horaApertura()));
                    valores.add(formatear(fila.fechaCierre(), fila.horaCierre()));
                    valores.add(fila.abrio());
                    valores.add(fila.cerro());
                    valores.add(fila.montoInicial());
                    valores.add(fila.estado().name());
                    for (String medio : medios) {
                        valores.addAll(valoresMedio(fila.valoresPorMedio().get(medio)));
                    }
                    return List.copyOf(valores);
                })
                .toList();
    }

    @Override
    public List<Object> totalesExportables() {
        List<Object> valores = new ArrayList<>();
        valores.add("Totales");
        for (int i = 1; i < 5; i++) {
            valores.add("");
        }
        // "Monto inicial" es una columna decimal: no puede llevar texto vacío.
        valores.add(CERO);
        valores.add(""); // Estado
        for (String medio : medios) {
            valores.addAll(valoresMedio(totales.porMedio().get(medio)));
        }
        return List.copyOf(valores);
    }

    private List<Object> valoresMedio(ValoresMedioPagoCierreDTO valores) {
        ValoresMedioPagoCierreDTO v = valores == null ? new ValoresMedioPagoCierreDTO(
                CERO, CERO, CERO, CERO, CERO, CERO, CERO) : valores;
        return List.of(v.esperado(), v.declarado(), v.diferencia(),
                v.ingresos(), v.egresos(), v.faltante(), v.sobrante());
    }

    private String formatear(LocalDate fecha, LocalTime hora) {
        if (fecha == null) {
            return "—";
        }
        return hora == null ? fecha.format(FECHA) : FECHA_HORA.format(fecha.atTime(hora));
    }

    private String etiquetaEstado(String valor) {
        if (valor == null || valor.isBlank() || "TODOS".equalsIgnoreCase(valor)) {
            return "Todas (cerradas y pendientes de revisión)";
        }
        if (EstadoSesionCaja.PENDIENTE_REVISION.name().equalsIgnoreCase(valor)) {
            return "Pendientes de revisión";
        }
        return "Cerradas";
    }

    private String etiquetaMedio(String valor) {
        if (valor == null || valor.isBlank() || "TODOS".equalsIgnoreCase(valor)) {
            return "Todos";
        }
        return valor.substring(0, 1).toUpperCase(Locale.ROOT)
                + valor.substring(1).toLowerCase(Locale.ROOT);
    }
}

package com.taller.gestion.auditoria.dto;

import com.taller.gestion.auditoria.entity.AccionAuditoria;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Alta manual de una operación de negocio (cierre de caja, anulación de venta, ajuste de
 * stock, etc.). La fecha y el usuario se toman de la sesión, no del cliente.
 */
@Schema(description = "Datos para registrar manualmente una operación de negocio en la auditoría")
public record AuditoriaCreateDTO(

        @NotNull(message = "La acción es obligatoria")
        @Schema(description = "Acción realizada", example = "CIERRE_CAJA")
        AccionAuditoria accion,

        @Size(max = 50, message = "El nombre de la tabla no puede superar los 50 caracteres")
        @Schema(description = "Tabla o recurso afectado", example = "Sesion_Caja")
        String tabla,

        @Schema(description = "Id del registro afectado", example = "12")
        Long id_registro,

        @Schema(description = "Cambios con formato {\"campo\": {\"anterior\": viejo, \"nuevo\": nuevo}}")
        Map<String, Object> cambios,

        @Size(max = 255, message = "El detalle no puede superar los 255 caracteres")
        @Schema(description = "Descripción libre de la operación", example = "Cierre con diferencia de $ 50")
        String detalle,

        @Schema(description = "En una corrección, id de la fila de auditoría que se enmienda",
                example = "123")
        Long id_auditoria_corregida
) {

    public AuditoriaCreateDTO(AccionAuditoria accion, String tabla, Long id_registro,
                              Map<String, Object> cambios, String detalle) {
        this(accion, tabla, id_registro, cambios, detalle, null);
    }
}

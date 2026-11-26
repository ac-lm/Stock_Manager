package com.taller.gestion.auditoria.dto;

import com.taller.gestion.auditoria.entity.AccionAuditoria;
import com.taller.gestion.auditoria.entity.OrigenAuditoria;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "Registro de auditoría. Las entidades se entregan siempre como DTO.")
public record AuditoriaResponseDTO(

        @Schema(description = "Id del registro de auditoría", example = "1")
        Long id_auditoria,

        @Schema(description = "Fecha y hora de la operación", example = "2026-11-26T10:15:30")
        LocalDateTime fecha_hora,

        @Schema(description = "Id del usuario que realizó la operación", example = "3")
        Long id_usuario,

        @Schema(description = "Email del usuario o SISTEMA", example = "ana@taller.local")
        String usuario,

        @Schema(description = "Acción registrada", example = "MODIFICACION")
        AccionAuditoria accion,

        @Schema(description = "Origen del registro", example = "MANUAL")
        OrigenAuditoria origen,

        @Schema(description = "Tabla o recurso afectado", example = "Cliente")
        String tabla,

        @Schema(description = "Id del registro afectado", example = "7")
        Long id_registro,

        @Schema(description = "En una corrección, id de la fila que se enmienda", example = "123")
        Long id_auditoria_corregida,

        @Schema(description = "Valor anterior y nuevo de cada campo modificado")
        Map<String, Object> cambios,

        @Schema(description = "Descripción libre de la operación")
        String detalle
) {
}

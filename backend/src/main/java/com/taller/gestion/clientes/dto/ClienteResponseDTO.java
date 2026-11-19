package com.taller.gestion.clientes.dto;

import com.taller.gestion.clientes.entity.TipoCliente;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos para mostrar la respuesta de un cliente")
public record ClienteResponseDTO(
        @Schema(description = "Identificador único del cliente, generado por el sistema. "
                + "Se usa en las rutas /clientes/{id} y para referenciar al cliente en otras operaciones",
                example = "1",
                accessMode = Schema.AccessMode.READ_ONLY)
        Long id_cliente,

        @Schema(description = "CUIT, con o sin guiones. Obligatorio para grandes clientes",
                example = "20-12345678-6", nullable = true)
        String cuit,

        @Schema(description = "Nombre o razón social", example = "Panadería Sol")
        String nombre,

        @Schema(description = "Teléfono de contacto", example = "2262-123456", nullable = true)
        String telefono,

        @Schema(description = "Correo electrónico", example = "contacto@panaderiasol.com")
        String email,

        @Schema(description = "Dirección", example = "Calle 1 123, Lobería")
        String direccion,

        @Schema(description = "Tipo de cliente. GENERICO está reservado para el sistema",
                allowableValues = {"GRAN_CLIENTE", "CONSUMIDOR_FINAL"}, example = "GRAN_CLIENTE")
        TipoCliente tipoCliente,

        @Schema(description = "Observaciones", nullable = true)
        String observaciones,

        @Schema(description = "Indica si el cliente está activo. Los inactivos no aparecen en las ventas nuevas "
                + "pero conservan su historial", example = "true")
        boolean activo,

        @Schema(description = "Plazo de pago en días. Solo para grandes clientes; vacío significa contado",
                example = "30", nullable = true)
        Integer plazoPagoDias
) { }

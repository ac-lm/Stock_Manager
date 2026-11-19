package com.taller.gestion.clientes.dto;

import com.taller.gestion.clientes.entity.TipoCliente;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Cliente con un nombre parecido al que se está registrando")
public record ClienteSimilarDTO(
        @Schema(description = "Identificador del cliente existente, para abrirlo en lugar de crear uno nuevo",
                example = "1")
        Long id_cliente,

        @Schema(description = "Nombre o razón social", example = "Panadería Sol")
        String nombre,

        @Schema(description = "CUIT, si tiene", example = "20-12345678-6", nullable = true)
        String cuit,

        @Schema(description = "Tipo de cliente", example = "GRAN_CLIENTE")
        TipoCliente tipoCliente,

        @Schema(description = "Si es false, el cliente existe pero está dado de baja y conviene reactivarlo",
                example = "true")
        boolean activo
) {}

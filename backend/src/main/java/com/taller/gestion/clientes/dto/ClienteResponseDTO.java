package com.taller.gestion.clientes.dto;

import com.taller.gestion.clientes.entity.TipoCliente;

public record ClienteResponseDTO(
        Long id_cliente,
        String cuit,
        String nombre,
        String telefono,
        String email,
        String direccion,
        TipoCliente tipo_cliente,
        String observaciones,
        boolean activo,
        Integer plazo_pago_dias
) { }

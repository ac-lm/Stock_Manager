package com.taller.gestion.clientes.dto;

import com.taller.gestion.clientes.entity.TipoCliente;

public record ClienteSimilarDTO(Long id, String nombre, String cuit,
                                TipoCliente tipo_cliente, boolean activo) {}

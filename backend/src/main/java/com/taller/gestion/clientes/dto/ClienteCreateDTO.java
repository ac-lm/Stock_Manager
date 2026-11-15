package com.taller.gestion.clientes.dto;

import com.taller.gestion.clientes.entity.TipoCliente;
import com.taller.gestion.util.TextoUtil;
import jakarta.validation.constraints.*;

public record ClienteCreateDTO(
        String cuit,
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 50)
        String nombre,
        @Size(max = 30)
        String telefono,
        @NotBlank(message = "El email es obligatorio")
        @Email
        @Size(max = 150)
        String email,
        @NotBlank(message = "La dirección es obligatoria")
        @Size(max = 80)
        String direccion,
        @NotNull(message = "El tipo de cliente es obligatorio")
        TipoCliente tipo_cliente,
        @Size(max = 250)
        String observaciones,
        @Min(value = 0, message = "El plazo no puede ser negativo")
        @Max(value = 365, message = "El plazo no puede superar los 365 días")
        Integer plazo_pago_dias
) {
        public ClienteCreateDTO {
                nombre = TextoUtil.limpiar(nombre);
                telefono = TextoUtil.limpiar(telefono);
                email = TextoUtil.limpiarEmail(email);
                direccion = TextoUtil.limpiar(direccion);
                observaciones = TextoUtil.limpiar(observaciones);
        }
}

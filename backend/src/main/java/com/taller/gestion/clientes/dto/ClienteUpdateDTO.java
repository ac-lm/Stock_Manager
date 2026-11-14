package com.taller.gestion.clientes.dto;

import com.taller.gestion.clientes.entity.TipoCliente;
import com.taller.gestion.common.util.TextoUtil;
import jakarta.validation.constraints.*;

public record ClienteUpdateDTO(
        @Pattern(regexp = "^\\d{2}-\\d{8}-\\d$", message = "El CUIT debe tener el formato XX-XXXXXXXX-X")
        String cuit,
        @NotBlank @Size(max = 50)
        String nombre,
        @Size(max = 30)
        String telefono,
        @NotBlank @Email @Size(max = 150)
        String email,
        @NotBlank @Size(max = 80)
        String direccion,
        @NotNull
        TipoCliente tipo_cliente,
        @Size(max = 250)
        String observaciones
) {
        public ClienteUpdateDTO {
                nombre = TextoUtil.limpiar(nombre);
                telefono = TextoUtil.limpiar(telefono);
                email = TextoUtil.limpiarEmail(email);
                direccion = TextoUtil.limpiar(direccion);
                observaciones = TextoUtil.limpiar(observaciones);
        }
}

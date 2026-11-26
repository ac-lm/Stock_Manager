package com.taller.gestion.reportes.exportacion;

import com.taller.gestion.common.exception.DatosInvalidosException;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Registro de los reportes exportables. Agregar uno nuevo no cambia el endpoint ni los generadores. */
@Component
public class RegistroProveedoresReporte {

    private final Map<String, ProveedorReporte> porId;

    public RegistroProveedoresReporte(List<ProveedorReporte> proveedores) {
        porId = proveedores.stream().collect(Collectors.toMap(
                ProveedorReporte::id,
                Function.identity(),
                (primero, segundo) -> {
                    throw new IllegalStateException("Reporte duplicado: " + primero.id());
                },
                LinkedHashMap::new));
    }

    public ProveedorReporte obtener(String id) {
        ProveedorReporte proveedor = id == null ? null : porId.get(id);
        if (proveedor == null) {
            throw new DatosInvalidosException("El reporte '" + id + "' no existe");
        }
        return proveedor;
    }
}

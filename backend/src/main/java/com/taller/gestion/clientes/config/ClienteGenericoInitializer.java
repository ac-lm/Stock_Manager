package com.taller.gestion.clientes.config;

import com.taller.gestion.clientes.entity.Cliente;
import com.taller.gestion.clientes.entity.TipoCliente;
import com.taller.gestion.clientes.repository.ClienteRepository;
import com.taller.gestion.util.TextoUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Crea el cliente reservado "Consumidor final" cuando no existe, para que las ventas de
 * mostrador tengan un cliente por defecto. Es idempotente: si ya está cargado, no hace nada.
 *
 * <p>El CUIT queda nulo porque un consumidor final no tiene CUIT. Email y dirección se completan
 * con valores de sistema porque la entidad los exige; no son datos de contacto reales.
 */
@Component
@RequiredArgsConstructor
public class ClienteGenericoInitializer implements ApplicationRunner {

    public static final String NOMBRE_GENERICO = "Consumidor final";

    private static final Logger log = LoggerFactory.getLogger(ClienteGenericoInitializer.class);
    private static final String EMAIL_SISTEMA = "consumidor.final@stockmanager.local";
    private static final String DIRECCION_SISTEMA = "Sin dirección";
    private static final String OBSERVACIONES = "Cliente reservado del sistema para ventas de "
            + "mostrador. No se edita ni se desactiva.";

    private final ClienteRepository clienteRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (clienteRepository.findByTipoCliente(TipoCliente.GENERICO).isPresent()) {
            return;
        }

        Cliente generico = new Cliente();
        generico.setNombre(NOMBRE_GENERICO);
        generico.setNombreNormalizado(TextoUtil.normalizarParaBusqueda(NOMBRE_GENERICO));
        generico.setCuit(null);
        generico.setEmail(EMAIL_SISTEMA);
        generico.setDireccion(DIRECCION_SISTEMA);
        generico.setTipoCliente(TipoCliente.GENERICO);
        generico.setObservaciones(OBSERVACIONES);
        generico.setPlazoPagoDias(null);
        generico.setActivo(true);

        clienteRepository.save(generico);
        log.info("Se creó el cliente reservado '{}'.", NOMBRE_GENERICO);
    }
}

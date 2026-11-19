package com.taller.gestion.costos.config;

import com.taller.gestion.costos.entity.TipoCosto;
import com.taller.gestion.costos.repository.TipoCostoRepository;
import com.taller.gestion.util.TextoUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * Crea el tipo de costo reservado "Insumos" cuando no existe, para que los ingresos de
 * mercadería puedan asociarle el costo que generan. Es idempotente: si el tipo ya está
 * cargado (o lo crea otra instancia al mismo tiempo), no hace nada.
 */
@Component
@RequiredArgsConstructor
public class TipoCostoInsumosInitializer implements ApplicationRunner {

    public static final String NOMBRE_INSUMOS = "Insumos";

    private static final Logger log = LoggerFactory.getLogger(TipoCostoInsumosInitializer.class);
    private static final String NOMBRE_NORMALIZADO = TextoUtil.normalizarParaBusqueda(NOMBRE_INSUMOS);

    private final TipoCostoRepository tipoCostoRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (tipoCostoRepository.existsByNombreNormalizado(NOMBRE_NORMALIZADO)) {
            return;
        }

        TipoCosto insumos = new TipoCosto();
        insumos.setNombre(NOMBRE_INSUMOS);
        insumos.setNombreNormalizado(NOMBRE_NORMALIZADO);
        insumos.setActivo(true);
        insumos.setReservado(true);

        try {
            tipoCostoRepository.save(insumos);
            log.info("Se creó el tipo de costo reservado '{}'.", NOMBRE_INSUMOS);
        } catch (DataIntegrityViolationException e) {
            // Otra instancia lo creó entre el exists y el save: el constraint único lo evita.
            log.debug("El tipo de costo '{}' ya fue creado por otra instancia.", NOMBRE_INSUMOS);
        }
    }
}

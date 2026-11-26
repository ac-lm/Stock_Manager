package com.taller.gestion.auditoria.config;

import com.taller.gestion.auditoria.listener.AuditoriaIntegrator;
import com.taller.gestion.auditoria.service.AuditoriaService;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.hibernate.jpa.boot.spi.JpaSettings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Registra el integrador de auditoría sin tocar la configuración de Hibernate ni las entidades
 * de los otros módulos. El servicio se resuelve de forma diferida para evitar un ciclo: el
 * customizer se crea antes del EntityManagerFactory del que dependen los repositorios.
 */
@Configuration
public class AuditoriaHibernateConfig {

    @Bean
    public HibernatePropertiesCustomizer auditoriaHibernatePropertiesCustomizer(
            ObjectProvider<AuditoriaService> auditoriaServiceProvider) {
        return properties -> properties.put(
                JpaSettings.INTEGRATOR_PROVIDER,
                (IntegratorProvider) () -> List.of(new AuditoriaIntegrator(auditoriaServiceProvider)));
    }
}

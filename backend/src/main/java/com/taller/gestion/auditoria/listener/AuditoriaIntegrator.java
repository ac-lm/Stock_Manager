package com.taller.gestion.auditoria.listener;

import com.taller.gestion.auditoria.service.AuditoriaService;
import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Engancha los eventos de JPA posteriores a insert/update/delete. Al estar registro desde el
 * módulo auditoria, ninguna entidad de los demás módulos necesita anotaciones ni listeners.
 */
public class AuditoriaIntegrator implements Integrator {

    private final ObjectProvider<AuditoriaService> auditoriaServiceProvider;

    public AuditoriaIntegrator(ObjectProvider<AuditoriaService> auditoriaServiceProvider) {
        this.auditoriaServiceProvider = auditoriaServiceProvider;
    }

    @Override
    public void integrate(Metadata metadata, SessionFactoryImplementor sessionFactory,
                          SessionFactoryServiceRegistry serviceRegistry) {
        EventListenerRegistry eventos = serviceRegistry.getService(EventListenerRegistry.class);
        AuditoriaEntityListener listener = new AuditoriaEntityListener(auditoriaServiceProvider);
        eventos.appendListeners(EventType.POST_INSERT, listener);
        eventos.appendListeners(EventType.POST_UPDATE, listener);
        eventos.appendListeners(EventType.POST_DELETE, listener);
    }

    @Override
    public void disintegrate(SessionFactoryImplementor sessionFactory,
                             SessionFactoryServiceRegistry serviceRegistry) {
        // No hay recursos propios que liberar.
    }
}

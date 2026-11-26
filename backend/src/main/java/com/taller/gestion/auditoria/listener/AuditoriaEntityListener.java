package com.taller.gestion.auditoria.listener;

import com.taller.gestion.auditoria.entity.AccionAuditoria;
import com.taller.gestion.auditoria.service.AuditoriaService;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Convierte cada alta, modificación o baja en una fila de {@code Auditoria} con el valor
 * anterior y el nuevo de los campos básicos que cambiaron. Se ignoran asociaciones,
 * colecciones y campos sensibles como la contraseña.
 */
public class AuditoriaEntityListener implements PostInsertEventListener,
        PostUpdateEventListener, PostDeleteEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaEntityListener.class);

    private final ObjectProvider<AuditoriaService> auditoriaServiceProvider;

    public AuditoriaEntityListener(ObjectProvider<AuditoriaService> auditoriaServiceProvider) {
        this.auditoriaServiceProvider = auditoriaServiceProvider;
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        EntityPersister persister = event.getPersister();
        if (esAuditoria(persister) || esInsercionInternaDeAuditoria(event)) {
            return;
        }
        Object[] estado = event.getState();
        Map<String, Object> cambios = new LinkedHashMap<>();
        for (int i = 0; i < estado.length; i++) {
            if (esCampoAuditable(persister, i) && estado[i] != null) {
                cambios.put(nombreColumna(persister.getPropertyNames()[i]), cambio(null, estado[i]));
            }
        }
        registrar(AccionAuditoria.ALTA, persister, event.getId(), cambios, null);
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        EntityPersister persister = event.getPersister();
        if (esAuditoria(persister)) {
            return;
        }
        Object[] anterior = event.getOldState();
        Object[] nuevo = event.getState();
        if (anterior == null || nuevo == null) {
            return;
        }

        Map<String, Object> cambios = new LinkedHashMap<>();
        int[] indices = event.getDirtyProperties();
        if (indices != null) {
            for (int i : indices) {
                if (esCampoAuditable(persister, i) && !Objects.equals(anterior[i], nuevo[i])) {
                    cambios.put(nombreColumna(persister.getPropertyNames()[i]),
                            cambio(anterior[i], nuevo[i]));
                }
            }
        } else {
            for (int i = 0; i < nuevo.length; i++) {
                if (esCampoAuditable(persister, i) && !Objects.equals(anterior[i], nuevo[i])) {
                    cambios.put(nombreColumna(persister.getPropertyNames()[i]),
                            cambio(anterior[i], nuevo[i]));
                }
            }
        }
        if (!cambios.isEmpty()) {
            registrar(AccionAuditoria.MODIFICACION, persister, event.getId(), cambios, null);
        }
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        EntityPersister persister = event.getPersister();
        if (esAuditoria(persister)) {
            return;
        }
        Object[] estado = event.getDeletedState();
        Map<String, Object> cambios = new LinkedHashMap<>();
        if (estado != null) {
            for (int i = 0; i < estado.length; i++) {
                if (esCampoAuditable(persister, i) && estado[i] != null) {
                    cambios.put(nombreColumna(persister.getPropertyNames()[i]), cambio(estado[i], null));
                }
            }
        }
        registrar(AccionAuditoria.BAJA, persister, event.getId(), cambios, null);
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        // Se escribe en la misma transacción que la operación: si esta se revierte,
        // también se revierte la fila de auditoría.
        return false;
    }

    private boolean esAuditoria(EntityPersister persister) {
        return "Auditoria".equals(nombreTabla(persister));
    }

    private boolean esInsercionInternaDeAuditoria(PostInsertEvent event) {
        return event.getEntity() != null
                && "Auditoria".equals(event.getEntity().getClass().getSimpleName());
    }

    private boolean esCampoAuditable(EntityPersister persister, int indice) {
        String[] nombres = persister.getPropertyNames();
        Type[] tipos = persister.getPropertyTypes();
        if (indice >= nombres.length || indice >= tipos.length) {
            return false;
        }
        Type tipo = tipos[indice];
        boolean esBasico = !tipo.isAssociationType() && !tipo.isCollectionType()
                && !tipo.isComponentType() && !tipo.isAnyType();
        return esBasico && !AuditoriaService.esCampoSensible(nombres[indice]);
    }

    private String nombreTabla(EntityPersister persister) {
        String nombre = persister.getEntityName();
        return nombre.contains(".") ? nombre.substring(nombre.lastIndexOf('.') + 1) : nombre;
    }

    /** Los campos se guardan con el nombre de columna del modelo (snake_case). */
    private String nombreColumna(String propertyName) {
        return propertyName.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT);
    }

    private void registrar(AccionAuditoria accion, EntityPersister persister, Object id,
                           Map<String, Object> cambios, String detalle) {
        try {
            auditoriaServiceProvider.getObject().registrarAutomatico(
                    accion, nombreTabla(persister), id instanceof Long valor ? valor : null,
                    cambios, detalle);
        } catch (RuntimeException ex) {
            // Nunca se debe impedir una operación de negocio porque falló la auditoría.
            log.warn("No se pudo registrar la auditoría de {} sobre {}", accion,
                    nombreTabla(persister), ex);
        }
    }

    private Map<String, Object> cambio(Object anterior, Object nuevo) {
        Map<String, Object> par = new LinkedHashMap<>();
        par.put("anterior", anterior);
        par.put("nuevo", nuevo);
        return par;
    }
}

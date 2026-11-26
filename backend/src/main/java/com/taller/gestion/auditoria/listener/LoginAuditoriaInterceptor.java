package com.taller.gestion.auditoria.listener;

import com.taller.gestion.auditoria.service.AuditoriaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Registra los inicios de sesión correctos sin tocar el controller de autenticación.
 * Se engancha solo en {@code POST /api/auth/login} y comprueba el estado de la respuesta:
 * si el login falló no se escribe nada.
 */
@Component
public class LoginAuditoriaInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LoginAuditoriaInterceptor.class);
    private static final AuthenticationTrustResolver TRUST_RESOLVER =
            new AuthenticationTrustResolverImpl();

    private final ObjectProvider<AuditoriaService> auditoriaServiceProvider;

    public LoginAuditoriaInterceptor(ObjectProvider<AuditoriaService> auditoriaServiceProvider) {
        this.auditoriaServiceProvider = auditoriaServiceProvider;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        if (ex != null || response.getStatus() != 200) {
            return;
        }
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()
                || TRUST_RESOLVER.isAnonymous(autenticacion)) {
            return;
        }
        try {
            AuditoriaService auditoriaService = auditoriaServiceProvider.getIfAvailable();
            if (auditoriaService != null) {
                auditoriaService.registrarLogin(autenticacion.getName());
            }
        } catch (RuntimeException error) {
            log.warn("No se pudo auditar el inicio de sesión de {}", autenticacion.getName(), error);
        }
    }
}

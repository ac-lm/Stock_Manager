package com.taller.gestion.auditoria.config;

import com.taller.gestion.auditoria.listener.LoginAuditoriaInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registra el interceptor de login únicamente para la ruta de autenticación.
 * El proveedor es diferido para que los {@code @WebMvcTest} de otros módulos, que no cargan
 * el service de auditoría, puedan seguir arrancando.
 */
@Configuration
@RequiredArgsConstructor
public class AuditoriaWebConfig implements WebMvcConfigurer {

    private final ObjectProvider<LoginAuditoriaInterceptor> loginAuditoriaInterceptorProvider;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        LoginAuditoriaInterceptor interceptor = loginAuditoriaInterceptorProvider.getIfAvailable();
        if (interceptor != null) {
            registry.addInterceptor(interceptor).addPathPatterns("/api/auth/login");
        }
    }
}

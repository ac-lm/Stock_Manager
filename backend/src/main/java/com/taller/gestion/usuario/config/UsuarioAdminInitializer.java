package com.taller.gestion.usuario.config;

import com.taller.gestion.usuario.dto.UsuarioCreateDTO;
import com.taller.gestion.usuario.entity.Rol;
import com.taller.gestion.usuario.repository.UsuarioRepository;
import com.taller.gestion.usuario.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Crea un administrador inicial cuando la tabla Usuario está vacía, para poder
 * iniciar sesión la primera vez. Si ya existe cualquier usuario, no hace nada.
 */
@Component
public class UsuarioAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UsuarioAdminInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final String email;
    private final String password;

    public UsuarioAdminInitializer(
            UsuarioRepository usuarioRepository,
            UsuarioService usuarioService,
            @Value("${app.admin-inicial.email}") String email,
            @Value("${app.admin-inicial.password}") String password) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.count() > 0) {
            return;
        }

        usuarioService.save(new UsuarioCreateDTO(
                "Administrador", email, password, Rol.ADMINISTRADOR));

        log.warn("Se creó el usuario administrador inicial '{}' porque la tabla Usuario estaba "
                + "vacía. Cambie la contraseña antes de usar el sistema en producción.", email);
    }
}

package com.taller.gestion.usuario.controller;

import com.taller.gestion.usuario.dto.UsuarioCreateDTO;
import com.taller.gestion.usuario.dto.UsuarioEstadoDTO;
import com.taller.gestion.usuario.dto.UsuarioPasswordDTO;
import com.taller.gestion.usuario.dto.UsuarioResponseDTO;
import com.taller.gestion.usuario.dto.UsuarioUpdateDTO;
import com.taller.gestion.usuario.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController implements UsuarioApi {

    private final UsuarioService usuarioService;

    @Override
    @GetMapping
    public Page<UsuarioResponseDTO> listar(
            @RequestParam(required = false) String busqueda,
            @RequestParam(defaultValue = "false") boolean incluirInactivos,
            @PageableDefault(size = 20, sort = "nombre") Pageable pageable) {
        return usuarioService.buscar(busqueda, incluirInactivos, pageable);
    }

    @Override
    @GetMapping("/{id_usuario}")
    public UsuarioResponseDTO obtener(@PathVariable Long id_usuario) {
        return usuarioService.findById(id_usuario);
    }

    @Override
    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> crear(@Valid @RequestBody UsuarioCreateDTO dto) {
        UsuarioResponseDTO nuevoUsuario = usuarioService.save(dto);
        return new ResponseEntity<>(nuevoUsuario, HttpStatus.CREATED);
    }

    @Override
    @PutMapping("/{id_usuario}")
    public UsuarioResponseDTO actualizar(@PathVariable Long id_usuario,
                                         @Valid @RequestBody UsuarioUpdateDTO dto) {
        return usuarioService.update(id_usuario, dto);
    }

    @Override
    @PatchMapping("/{id_usuario}/estado")
    public UsuarioResponseDTO cambiarEstado(@PathVariable Long id_usuario,
                                            @Valid @RequestBody UsuarioEstadoDTO dto) {
        return usuarioService.cambiarEstado(id_usuario, dto.activo());
    }

    @Override
    @PutMapping("/{id_usuario}/password")
    public ResponseEntity<Void> cambiarPassword(@PathVariable Long id_usuario,
                                                @Valid @RequestBody UsuarioPasswordDTO dto) {
        usuarioService.cambiarPassword(id_usuario, dto);
        return ResponseEntity.noContent().build();
    }
}


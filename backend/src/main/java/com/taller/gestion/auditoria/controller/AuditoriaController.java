package com.taller.gestion.auditoria.controller;

import com.taller.gestion.auditoria.dto.AuditoriaCreateDTO;
import com.taller.gestion.auditoria.dto.AuditoriaResponseDTO;
import com.taller.gestion.auditoria.entity.AccionAuditoria;
import com.taller.gestion.auditoria.service.AuditoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auditoria")
@RequiredArgsConstructor
public class AuditoriaController implements AuditoriaApi {

    private final AuditoriaService auditoriaService;

    @Override
    @GetMapping
    public Page<AuditoriaResponseDTO> listar(
            @RequestParam(required = false) Long id_usuario,
            @RequestParam(required = false) AccionAuditoria accion,
            @RequestParam(required = false) String tabla,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @PageableDefault(size = 20) Pageable pageable) {
        return auditoriaService.buscar(id_usuario, accion, tabla, desde, hasta, pageable);
    }

    @Override
    @GetMapping("/{id_auditoria}")
    public AuditoriaResponseDTO obtener(@PathVariable Long id_auditoria) {
        return auditoriaService.obtener(id_auditoria);
    }

    @Override
    @PostMapping
    public ResponseEntity<AuditoriaResponseDTO> crear(@Valid @RequestBody AuditoriaCreateDTO dto) {
        return new ResponseEntity<>(auditoriaService.crear(dto), HttpStatus.CREATED);
    }
}

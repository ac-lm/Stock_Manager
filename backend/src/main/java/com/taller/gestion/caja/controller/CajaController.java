package com.taller.gestion.caja.controller;

import com.taller.gestion.caja.dto.AbrirCajaDTO;
import com.taller.gestion.caja.dto.CajaActualDTO;
import com.taller.gestion.caja.dto.CerrarCajaDTO;
import com.taller.gestion.caja.dto.MovimientoCajaCreateDTO;
import com.taller.gestion.caja.dto.MovimientoCajaResponseDTO;
import com.taller.gestion.caja.dto.RevisionCajaDTO;
import com.taller.gestion.caja.dto.SesionCajaDetalleDTO;
import com.taller.gestion.caja.dto.SesionCajaResponseDTO;
import com.taller.gestion.caja.entity.EstadoSesionCaja;
import com.taller.gestion.caja.service.CajaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

import java.time.LocalDate;

@RestController
@RequestMapping("/api/caja")
@RequiredArgsConstructor
public class CajaController implements CajaApi {

    private final CajaService cajaService;

    @GetMapping("/actual")
    public CajaActualDTO actual() {
        return cajaService.actual();
    }

    @PostMapping("/sesiones")
    public ResponseEntity<SesionCajaResponseDTO> abrir(@Valid @RequestBody AbrirCajaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cajaService.abrir(dto));
    }

    @PostMapping("/sesiones/{id_sesion}/movimientos")
    public ResponseEntity<MovimientoCajaResponseDTO> registrarEgreso(
            @PathVariable Long id_sesion,
            @Valid @RequestBody MovimientoCajaCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cajaService.registrarEgreso(id_sesion, dto));
    }

    @PostMapping("/sesiones/{id_sesion}/cierre")
    public SesionCajaResponseDTO cerrar(
            @PathVariable Long id_sesion,
            @Valid @RequestBody CerrarCajaDTO dto) {
        return cajaService.cerrar(id_sesion, dto);
    }

    @PostMapping("/sesiones/{id_sesion}/revision")
    public SesionCajaResponseDTO revisar(
            @PathVariable Long id_sesion,
            @Valid @RequestBody RevisionCajaDTO dto) {
        return cajaService.revisar(id_sesion, dto);
    }

    @GetMapping("/sesiones")
    public Page<SesionCajaResponseDTO> listar(
            @RequestParam(required = false) EstadoSesionCaja estado,
            @RequestParam(required = false) Long id_usuario,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 10, sort = {"fechaApertura", "horaApertura"},
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return cajaService.listar(estado, id_usuario, desde, hasta, pageable);
    }

    @GetMapping("/sesiones/{id_sesion}")
    public SesionCajaDetalleDTO obtener(@PathVariable Long id_sesion) {
        return cajaService.obtenerDetalle(id_sesion);
    }
}

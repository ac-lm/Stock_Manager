package com.taller.gestion.clientes.controller;

import com.taller.gestion.clientes.dto.*;
import com.taller.gestion.clientes.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController implements ClienteApi {
    private final ClienteService clienteService;

    @PreAuthorize("hasAnyRole('ADMINISTRADOR','VENDEDOR')")
    @GetMapping
    public Page<ClienteResponseDTO> listar(
            @RequestParam(required = false) String busqueda,
            @RequestParam(defaultValue = "false") boolean incluirInactivos,
            @PageableDefault(size = 20, sort = "nombre") Pageable pageable) {
        return clienteService.buscar(busqueda, incluirInactivos, pageable);
    }

    @PostMapping
    public ResponseEntity<ClienteResponseDTO> crear(@Valid @RequestBody ClienteCreateDTO dto) {
        ClienteResponseDTO nuevoCliente = clienteService.save(dto);
        return new ResponseEntity<>(nuevoCliente, HttpStatus.CREATED);
    }

    @GetMapping("/similares")
    public List<ClienteSimilarDTO> similares(@RequestParam String nombre) {
        return clienteService.buscarSimilares(nombre);
    }

    @GetMapping("/{id_cliente}")
    public ClienteResponseDTO obtener(@PathVariable Long id_cliente) {
        return clienteService.findById(id_cliente);
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADOR','VENDEDOR')")
    @PutMapping("/{id_cliente}")
    public ClienteResponseDTO actualizar(@PathVariable Long id_cliente,
                                         @Valid @RequestBody ClienteUpdateDTO dto) {
        return clienteService.update(id_cliente, dto);
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADOR','VENDEDOR')")
    @PatchMapping("/{id_cliente}/estado")
    public ClienteResponseDTO cambiarEstado(@PathVariable Long id_cliente,
                                            @Valid @RequestBody ClienteEstadoDTO dto) {
        return clienteService.cambiarEstado(id_cliente, dto.activo());
    }
}

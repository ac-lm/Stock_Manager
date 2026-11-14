package com.taller.gestion.clientes.controller;

import com.taller.gestion.clientes.dto.ClienteCreateDTO;
import com.taller.gestion.clientes.dto.ClienteEstadoDTO;
import com.taller.gestion.clientes.dto.ClienteResponseDTO;
import com.taller.gestion.clientes.dto.ClienteUpdateDTO;
import com.taller.gestion.clientes.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {
    private final ClienteService clienteService;

    @GetMapping
    public Page<ClienteResponseDTO> listar(
            @RequestParam(required = false) String busqueda,
            @RequestParam(defaultValue = "false") boolean incluirInactivos,
            @PageableDefault(size = 20, sort = "nombre") Pageable pageable) {
        return clienteService.buscar(busqueda, incluirInactivos, pageable);
    }

    @PostMapping
    public ResponseEntity<ClienteResponseDTO> crear(@RequestBody ClienteCreateDTO dto) {
        ClienteResponseDTO nuevoCliente = clienteService.save(dto);
        return new ResponseEntity<>(nuevoCliente, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ClienteResponseDTO obtener(@PathVariable Long id) {
        return clienteService.findById(id);
    }

    @PutMapping("/{id}")
    public ClienteResponseDTO actualizar(@PathVariable Long id,
                                         @Valid @RequestBody ClienteUpdateDTO dto) {
        return clienteService.update(id, dto);
    }

    @PatchMapping("/{id}/estado")
    public ClienteResponseDTO cambiarEstado(@PathVariable Long id,
                                            @Valid @RequestBody ClienteEstadoDTO dto) {
        return clienteService.cambiarEstado(id, dto.activo());
    }
}

package com.taller.gestion.venta.controller;

import com.taller.gestion.venta.dto.VentaCreateDTO;
import com.taller.gestion.venta.dto.VentaResponseDTO;
import com.taller.gestion.venta.entity.EstadoVenta;
import com.taller.gestion.venta.service.VentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
@Tag(name = "Ventas", description = "Alta, consulta y anulación de ventas. La venta se confirma "
        + "al cargarla: no existe estado pendiente. El vendedor se toma de la sesión y el precio "
        + "de la lista del tipo de cliente. Requiere caja abierta: cada pago genera un ingreso "
        + "en la sesión. No se descuenta stock ni se generan comprobantes.")
public class VentaController {

    private final VentaService ventaService;

    @PostMapping
    @Operation(summary = "Confirmar una venta", description = "Arma y confirma la venta en un solo "
            + "paso. Si no se manda id_cliente se usa el genérico 'Consumidor final'. Requiere "
            + "una sesión de caja ABIERTA y pagos cuya suma iguale el total calculado; cada pago "
            + "se registra como un movimiento INGRESO/VENTA en la misma transacción.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Venta confirmada",
                    content = @Content(schema = @Schema(implementation = VentaResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o pagos que no suman el total"),
            @ApiResponse(responseCode = "404", description = "Cliente o producto no encontrado"),
            @ApiResponse(responseCode = "409", description = "Cliente inactivo o caja cerrada")
    })
    public ResponseEntity<VentaResponseDTO> crear(@Valid @RequestBody VentaCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ventaService.crearVenta(dto));
    }

    @GetMapping
    @Operation(summary = "Listar ventas", description = "Filtros opcionales por estado, cliente "
            + "y rango de fechas.")
    public Page<VentaResponseDTO> listar(
            @RequestParam(required = false) EstadoVenta estado,
            @RequestParam(required = false) Long id_cliente,
            @Parameter(description = "Fecha desde (inclusive)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(description = "Fecha hasta (inclusive)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 20, sort = "fecha", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ventaService.listarVentas(estado, id_cliente, desde, hasta, pageable);
    }

    @GetMapping("/{id_venta}")
    @Operation(summary = "Obtener una venta por id")
    @ApiResponse(responseCode = "404", description = "Venta no encontrada")
    public VentaResponseDTO obtener(@PathVariable Long id_venta) {
        return ventaService.obtenerVenta(id_venta);
    }

    @PutMapping("/{id_venta}/anular")
    @Operation(summary = "Anular una venta", description = "Cambia el estado a ANULADA. La venta "
            + "no se borra y no se revierte stock.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Venta anulada"),
            @ApiResponse(responseCode = "404", description = "Venta no encontrada"),
            @ApiResponse(responseCode = "409", description = "La venta ya estaba anulada")
    })
    public VentaResponseDTO anular(@PathVariable Long id_venta) {
        return ventaService.anularVenta(id_venta);
    }
}

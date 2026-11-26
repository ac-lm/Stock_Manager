package com.taller.gestion.stock.controller;

import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.stock.dto.MovimientoStockCreateDTO;
import com.taller.gestion.stock.dto.MovimientoStockResponseDTO;
import com.taller.gestion.stock.dto.StockInsumoResponseDTO;
import com.taller.gestion.stock.dto.StockProductoResponseDTO;
import com.taller.gestion.stock.entity.MotivoMovimientoStock;
import com.taller.gestion.stock.entity.TipoMovimientoStock;
import com.taller.gestion.stock.service.StockService;
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

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController implements StockApi {

    private final StockService stockService;

    @GetMapping("/insumos")
    public List<StockInsumoResponseDTO> listarInsumos() {
        return stockService.listarInsumos();
    }

    @GetMapping("/productos")
    public List<StockProductoResponseDTO> listarProductos() {
        return stockService.listarProductos();
    }

    @PostMapping("/insumos/movimientos")
    public ResponseEntity<MovimientoStockResponseDTO> registrarMovimientoInsumo(
            @Valid @RequestBody MovimientoStockCreateDTO dto) {
        if (dto.idInsumo() == null) {
            throw new DatosInvalidosException("El id del insumo es obligatorio");
        }
        if (dto.motivo() == MotivoMovimientoStock.INICIAL) {
            throw new DatosInvalidosException(
                    "El motivo INICIAL se genera automáticamente al dar de alta en el catálogo");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(stockService.registrarMovimiento(
                null, dto.idInsumo(), dto.tipo(), dto.motivo(), dto.cantidad(),
                dto.observaciones()));
    }

    @PostMapping("/productos/movimientos")
    public ResponseEntity<MovimientoStockResponseDTO> registrarMovimientoProducto(
            @Valid @RequestBody MovimientoStockCreateDTO dto) {
        if (dto.idProducto() == null) {
            throw new DatosInvalidosException("El id del producto es obligatorio");
        }
        if (dto.motivo() == MotivoMovimientoStock.INICIAL) {
            throw new DatosInvalidosException(
                    "El motivo INICIAL se genera automáticamente al dar de alta en el catálogo");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(stockService.registrarMovimiento(
                dto.idProducto(), null, dto.tipo(), dto.motivo(), dto.cantidad(),
                dto.observaciones()));
    }

    @GetMapping("/insumos/{id_insumo}/movimientos")
    public List<MovimientoStockResponseDTO> kardexInsumo(@PathVariable Long id_insumo) {
        return stockService.kardexInsumo(id_insumo);
    }

    @GetMapping("/productos/{id_producto}/movimientos")
    public List<MovimientoStockResponseDTO> kardexProducto(@PathVariable Long id_producto) {
        return stockService.kardexProducto(id_producto);
    }

    @GetMapping("/movimientos")
    public Page<MovimientoStockResponseDTO> historial(
            @RequestParam(required = false) TipoMovimientoStock tipo,
            @RequestParam(required = false) MotivoMovimientoStock motivo,
            @RequestParam(required = false) Long id_producto,
            @RequestParam(required = false) Long id_insumo,
            @RequestParam(required = false) Long id_usuario,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @PageableDefault(size = 10, sort = {"fechaHora"},
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return stockService.historial(
                tipo, motivo, id_producto, id_insumo, id_usuario, desde, hasta, pageable);
    }
}

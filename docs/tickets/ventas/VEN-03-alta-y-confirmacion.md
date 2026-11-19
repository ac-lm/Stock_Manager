# VEN-03. Alta y confirmación de venta

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | `VEN-01`, `VEN-02`, `CLI-09` |
| Bloquea | `VEN-04`, `VEN-05`, `VEN-11`, `VEN-12` |
| Etiquetas | `ventas`, `backend` |

**Historia:** como vendedor, quiero cargar una venta rápido, incluso sin cliente cargado, y que
quede confirmada al guardar.

## Qué construir

- `POST /api/ventas`.
- DTOs `VentaCreateDTO`, `DetalleVentaCreateDTO` y de respuesta.
- Resolución de cliente, vendedor, precios y totales en `VentaService`.

## Criterios de aceptación

- [x] Sin `id_cliente` usa el genérico "Consumidor final" (`CLI-09`).
- [x] Cliente inactivo responde 409.
- [x] `id_vendedor` se toma de `UsuarioService.obtenerAutenticado()`, nunca del body.
- [x] Fecha y hora las pone el servidor.
- [x] Al menos un producto; cantidad decimal y mayor a cero.
- [x] Precio por tipo de cliente; no se puede editar; sin descuento.
- [x] Subtotal y total calculados en backend con `BigDecimal`.
- [x] `fecha_vencimiento` opcional (saldo con seña, `VEN-09`).
- [x] No se descuenta stock y no se generan comprobantes.
- [x] Queda `CONFIRMADA`.

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/ventas` | Confirmar una venta |

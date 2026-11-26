# VEN-10. Descuento de stock al confirmar

| Campo | Valor |
|---|---|
| Estado | 🔴 Pendiente |
| Bloqueado por | — (`STOCK-01` ya está implementado; falta desarrollar este ticket) |
| Bloquea | — |
| Etiquetas | `ventas`, `stock`, `backend` |

**Historia:** como sistema, quiero que el stock baje al confirmar una venta, dejando un
`Movimiento_Stock` auditable.

## Qué construir

- Descontar `cantidad` de `Producto.stockActual` por cada renglón.
- Registrar `Movimiento_Stock` con `id_venta`, usuario y fecha/hora.

## Criterios de aceptación

- [ ] El descuento ocurre en la misma transacción que la venta.
- [ ] Falta de stock **bloquea** con `409`: la venta no se confirma y no se registra movimiento.
      No se permite stock negativo.
- [ ] Anular una venta revierte el movimiento. CAJA-07 ya está listo: busca las `SALIDA` con
      `id_venta` y genera una `ENTRADA`/`DEVOLUCION` por cada una; este ticket solo debe asegurar
      que sus salidas se registren con esa FK.
- [ ] No se descuenta dos veces si se reintenta el alta.

## Notas

La decisión 8b del relevamiento (confirmar con stock negativo) quedó **revertida el 26/11/2026**:
prevalece la regla general "`stock` nunca negativo", igual que en `incidencias` (`409`). Ver
[`preguntas.md`](preguntas.md#8-si-al-vender-no-alcanza-el-stock-qué-pasa). El DDL ya trae
`Movimiento_Stock` con `id_venta` nullable, ya usado por CAJA-07 para las devoluciones.

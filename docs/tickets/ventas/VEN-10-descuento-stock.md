# VEN-10. Descuento de stock al confirmar

| Campo | Valor |
|---|---|
| Estado | 🔴 Bloqueado |
| Bloqueado por | módulo `stock` (`STOCK-01`) |
| Bloquea | — |
| Etiquetas | `ventas`, `stock`, `backend` |

**Historia:** como sistema, quiero que el stock baje al confirmar una venta, dejando un
`Movimiento_Stock` auditable.

## Qué construir

- Descontar `cantidad` de `Producto.stockActual` por cada renglón.
- Registrar `Movimiento_Stock` con `id_venta`, usuario y fecha/hora.

## Criterios de aceptación

- [ ] El descuento ocurre en la misma transacción que la venta.
- [ ] Falta de stock **no bloquea** (decisión 8b): se confirma con aviso y el saldo queda negativo.
- [ ] Anular una venta define si se revierte el movimiento.
- [ ] No se descuenta dos veces si se reintenta el alta.

## Notas

Esta regla contradice la regla general de `PROMPT_CONTEXTO` ("stock nunca negativo") y lo hecho en
`incidencias` (409). Prevalece la decisión del relevamiento. El DDL ya trae `Movimiento_Stock` con
`id_venta` nullable.

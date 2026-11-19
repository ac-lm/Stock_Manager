# VEN-09. Venta desde pedido con seña

| Campo | Valor |
|---|---|
| Estado | 🔴 Bloqueado |
| Bloqueado por | módulos `pedidos` y `caja` |
| Bloquea | — |
| Etiquetas | `ventas`, `pedidos`, `caja`, `backend`, `db` |

**Historia:** como vendedor, al entregar un pedido quiero generar la venta con sus productos y
precios, sin cargarla dos veces.

## Qué construir

- Agregar `id_pedido` a `Venta` (hoy no está en el DDL).
- Generar la venta al entregar el pedido, tomando productos y precios del pedido.
- Registrar la seña: entra a caja al aprobar el pedido y se descuenta al entregar.
- Usar `fecha_vencimiento` para el plazo del saldo.

## Criterios de aceptación

- [ ] La venta guarda `id_pedido` y no se carga dos veces.
- [ ] Los renglones y precios vienen del pedido.
- [ ] La seña se contabiliza en caja y se descuenta del total.
- [ ] Se define qué pasa si el pedido se anula después de generar la venta.

## Notas

Decisión registrada en las aclaraciones 7 y 11 de [`preguntas.md`](preguntas.md). El DDL actual
tiene `Pedido.descuento` pero no `seña`, y `Venta` no tiene `id_pedido`.

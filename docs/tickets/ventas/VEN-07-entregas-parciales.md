# VEN-07. Entregas parciales con Detalle_Remito

| Campo | Valor |
|---|---|
| Estado | 🔴 Bloqueado |
| Bloqueado por | `VEN-06` |
| Bloquea | — |
| Etiquetas | `ventas`, `backend`, `db` |

**Historia:** como cliente, quiero recibir una venta en varias entregas y saber qué se entregó en
cada remito.

## Qué construir

- Carga de remitos parciales con `Detalle_Remito`.
- Control de que lo entregado no supere lo vendido.

## Criterios de aceptación

- [ ] Un mismo remito puede tener varios renglones.
- [ ] La suma entregada por producto no supera `Detalle_Venta.cantidad`.
- [ ] Se puede consultar lo pendiente de entrega.

## Notas del modelo

El DDL ya trae `Detalle_Remito.id_detalle_remito serial`, `numero_remito`, `id_venta` y
`observaciones`. **`cantidad` sigue siendo `int`**: Vertabelo tira error al ponerla decimal, hay
que revisar por qué antes de vender por peso.

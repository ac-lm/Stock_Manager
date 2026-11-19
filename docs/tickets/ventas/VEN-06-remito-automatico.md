# VEN-06. Remito automático por venta

| Campo | Valor |
|---|---|
| Estado | 🔴 Bloqueado |
| Bloqueado por | `VEN-03`, módulo de comprobantes |
| Bloquea | `VEN-07`, `VEN-08` |
| Etiquetas | `ventas`, `backend`, `db`, `diseño` |

**Historia:** como cliente, quiero un remito por cada venta sin pedirlo.

## Qué construir

- Generar `Remito_Comprobante` al confirmar la venta, en la misma transacción.
- Número automático, fecha, dirección de entrega y cliente.

## Criterios de aceptación

- [ ] Toda venta confirmada tiene un remito asociado.
- [ ] El remito se genera dentro de la transacción de alta.
- [ ] Anular la venta define el destino del remito (¿se anula también?).
- [ ] No se genera PDF en este ticket.

## Tablas

`Remito_Comprobante`, `Detalle_Remito`.

## Notas del modelo

El DDL ya trae la FK `Remito_Comprobante_Cliente` y `id_detalle_remito serial`. Falta definir quién
emite el número y si la dirección de entrega se copia del cliente o se pide al vender.

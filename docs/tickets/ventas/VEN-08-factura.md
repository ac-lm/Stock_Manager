# VEN-08. Emisión de factura

| Campo | Valor |
|---|---|
| Estado | 🔴 Bloqueado |
| Bloqueado por | `VEN-06`, módulo de facturación |
| Bloquea | — |
| Etiquetas | `ventas`, `backend`, `diseño` |

**Historia:** como cliente, quiero que el sistema emita la factura de mi venta.

## Qué construir

- Definir el modelo de factura en Vertabelo/Redgate (hoy no existe ninguna tabla de factura).
- Emitir la factura desde la venta o el remito, con su numeración.

## Criterios de aceptación

- [ ] La factura queda asociada a la venta y al cliente.
- [ ] Numeración secuencial sin duplicados.
- [ ] Dar de baja una venta define qué pasa con la factura (¿nota de crédito?).
- [ ] Queda definido si se genera PDF o solo el número.

## Notas

Requiere decisión de negocio sobre punto de venta, numeración y fechas de emisión. No hay tabla en
el DDL actual.

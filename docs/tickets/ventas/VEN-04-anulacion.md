# VEN-04. Anulación de venta

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | `VEN-03` |
| Bloquea | — |
| Etiquetas | `ventas`, `backend` |

**Historia:** como administrador, quiero anular una venta sin borrar su historial.

## Qué construir

- `PUT /api/ventas/{id_venta}/anular`.

## Criterios de aceptación

- [x] Pasa de `CONFIRMADA` a `ANULADA`.
- [x] La venta no se borra.
- [x] Un id inexistente responde 404.
- [x] Anular dos veces responde 409.
- [x] Pide un motivo obligatorio y revierte los cobros con contramovimientos de caja (CAJA-07).
- [x] Devuelve stock solo si la venta registró `SALIDA` con `id_venta`; hoy `VEN-10` no descuenta,
  así que anular no crea mercadería.

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| `PUT` | `/api/ventas/{id_venta}/anular` | Anular una venta con body `{ "motivo": "..." }` |

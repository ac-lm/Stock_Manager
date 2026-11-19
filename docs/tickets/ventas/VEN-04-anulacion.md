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
- [x] No revierte stock: la venta nunca lo descontó (`VEN-10`).

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| `PUT` | `/api/ventas/{id_venta}/anular` | Anular una venta |

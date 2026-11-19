# VEN-01. Modelo de Venta y Detalle_Venta

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | DDL recibido (2026-11-19) |
| Bloquea | `VEN-03`, `VEN-05`, `VEN-11`, `VEN-12` |
| Etiquetas | `ventas`, `backend`, `db` |

**Historia:** como sistema, necesito persistir una venta con sus renglones respetando el modelo
de datos, sin campos inventados.

## Qué construir

- Entidades `Venta`, `DetalleVenta` y enum `EstadoVenta`.
- Repositorios `VentaRepository` y `PrecioTipoClienteRepository` (este último de `VEN-02`).
- Reemplazar el esqueleto anterior (`Double`, `estado` como `String`, sin detalle).

## Criterios de aceptación

- [x] `Venta`: `id_venta`, `id_cliente`, `id_vendedor`, `fecha`, `hora`, `total decimal(12,2)`,
      `estado`, `fecha_vencimiento` nullable.
- [x] `Detalle_Venta`: `id_detalle_venta` propio (PK), `id_venta_fk`, `id_producto_fk`,
      `cantidad decimal(12,3)`, `precio_historico`, `subtotal`.
- [x] Sin `descuento` ni `medio_pago` (decidido en Caja).
- [x] Estados `CONFIRMADA` / `ANULADA`; no existe `PENDIENTE`.
- [x] Los renglones se guardan en cascada con la venta.

## Tablas

`Venta`, `Detalle_Venta`.

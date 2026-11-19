# VEN-13. Frontend de ventas

| Campo | Valor |
|---|---|
| Estado | 🔴 Bloqueado |
| Bloqueado por | `VEN-03`, `VEN-05` |
| Bloquea | — |
| Etiquetas | `ventas`, `frontend` |

**Historia:** como vendedor, quiero una pantalla para cargar ventas rápido, con un botón de
"Consumidor final" y otro para buscar un cliente.

## Qué construir

- Pantalla de alta de venta con renglones (producto + cantidad).
- Selector de cliente con opción "Consumidor final" (id_cliente vacío).
- Listado con filtros y botón de anulación.

## Criterios de aceptación

- [ ] Se pueden agregar/quitar renglones y se ve el total estimado.
- [ ] No existe campo de precio ni de descuento.
- [ ] El seller no se muestra: sale de la sesión.
- [ ] Anular pide confirmación y avisa que no se revierte stock.
- [ ] `npm run lint` y `npm run build` pasan.

## Notas

El backend ya está listo. Se hace después del frontend de incidencias, según el orden acordado.

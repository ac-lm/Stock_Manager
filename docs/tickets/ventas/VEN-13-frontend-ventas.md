# VEN-13. Frontend de ventas

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | `VEN-03`, `VEN-05` (resueltos) |
| Bloquea | — |
| Etiquetas | `ventas`, `frontend` |

**Historia:** como vendedor, quiero una pantalla para cargar ventas rápido, con un botón de
"Consumidor final" y otro para buscar un cliente.

## Qué construir

- Pantalla de alta de venta con renglones (producto + cantidad).
- Selector de cliente con opción "Consumidor final" (id_cliente vacío).
- Listado con filtros y botón de anulación.

## Criterios de aceptación

- [x] Se pueden agregar/quitar renglones y se ve el total estimado.
- [x] No existe campo de precio ni de descuento.
- [x] El vendedor no se muestra: sale de la sesión.
- [x] Anular pide confirmación y avisa que no se revierte stock.
- [x] `npm run lint` y `npm run build` pasan.

## Archivos

- `frontend/src/services/ventasService.js`
- `frontend/src/pages/VentasPage.jsx`
- `frontend/src/App.jsx`: ruta `/ventas`.
- `frontend/src/components/Navbar.jsx`: link **Ventas**.

## Notas

El total estimado usa precios de catálogo y está aclarado en la pantalla: el total real lo calcula
el backend con la lista del tipo de cliente. Queda pendiente la optimización del N+1 de `VEN-05`.

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
- Con CAJA-02: pagos mixtos (efectivo/transferencia), aviso y bloqueo sin caja abierta, y pagos
  registrados en el detalle de la venta.

## Criterios de aceptación

- [x] Se pueden agregar/quitar renglones y se ve el total estimado.
- [x] No existe campo de precio ni de descuento.
- [x] El vendedor no se muestra: sale de la sesión.
- [x] Anular abre un modal con motivo obligatorio y avisa que se revierten los cobros y el stock
  descontado, si lo hubo (CAJA-07).
- [x] El alta deshabilita "Confirmar venta" si no hay caja abierta (CAJA-02).
- [x] Se pueden cargar varios pagos y se muestra el total pagado (CAJA-02).
- [x] El detalle muestra los pagos registrados en caja (CAJA-02).
- [x] `npm run lint` y `npm run build` pasan.

## Archivos

- `frontend/src/services/ventasService.js`
- `frontend/src/pages/VentasPage.jsx`
- `frontend/src/App.jsx`: ruta `/ventas`.
- `frontend/src/components/Navbar.jsx`: link **Ventas**.

## Notas

El total estimado usa precios de catálogo y está aclarado en la pantalla: el total real lo calcula
el backend con la lista del tipo de cliente. Queda pendiente la optimización del N+1 de `VEN-05`.

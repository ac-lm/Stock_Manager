# VEN-05. Consultas y filtros de ventas

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | `VEN-03` |
| Bloquea | `VEN-13` |
| Etiquetas | `ventas`, `backend` |

**Historia:** como vendedor, quiero buscar ventas por estado, cliente y fecha para consultar lo
que hice.

## Qué construir

- `GET /api/ventas` con filtros opcionales y paginación.
- `GET /api/ventas/{id_venta}`.

## Criterios de aceptación

- [x] Filtros: `estado`, `id_cliente`, `desde`, `hasta` (fecha, inclusivos).
- [x] Paginación con `Pageable`, 20 por página, orden por fecha descendente.
- [x] La respuesta incluye los renglones de cada venta.
- [x] Id inexistente responde 404.
- [ ] Optimizar el nombre del cliente: hoy se consulta por venta (N+1); resolver con un join o
      un campo snapshot.

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/ventas` | Listar ventas con filtros |
| `GET` | `/api/ventas/{id_venta}` | Obtener una venta |

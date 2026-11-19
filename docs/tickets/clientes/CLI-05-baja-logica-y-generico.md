# CLI-05. Baja lógica y cliente genérico

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | `CLI-01` |
| Bloquea | `CLI-06`, `CLI-07`, `CLI-08` |
| Etiquetas | `clientes`, `backend`, `frontend` |

**Historia:** como administrador, quiero desactivar clientes sin perder su historial y proteger al
cliente genérico del sistema.

## Qué construir

- `PATCH /api/clientes/{id_cliente}/estado`.
- Reglas de negocio para el cliente `GENERICO`.
- Botones **Desactivar** / **Reactivar** en la pantalla de clientes.

## Criterios de aceptación

- [x] El body pide `activo`; si falta, responde 400.
- [x] `activo = false` da de baja lógica: el cliente no se borra y conserva su historial.
- [x] `activo = true` reactiva al cliente.
- [x] Un id inexistente responde 404.
- [x] El cliente `GENERICO` no se puede modificar ni desactivar (409).
- [x] El listado permite ver los inactivos con el checkbox **Mostrar inactivos**.
- [x] Al desactivar, el frontend pide confirmación y avisa que el cliente dejará de aparecer al
      vender.
- [x] El cliente genérico se muestra como **Reservado** y sin acciones.

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| `PATCH` | `/api/clientes/{id_cliente}/estado` | Activa o desactiva un cliente |

## Tablas

`Cliente`.

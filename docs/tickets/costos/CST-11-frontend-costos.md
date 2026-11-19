# CST-11. Pantalla de costos

| Campo | Valor |
|---|---|
| Estado | 🟢 Listo |
| Bloqueado por | `CST-02`, `CST-03`, `CST-04`, `CST-07` |
| Bloquea | — |
| Etiquetas | `costos`, `frontend` |

**Historia:** como administrador, quiero una pantalla para cargar, filtrar y anular costos y
administrar sus tipos sin usar Swagger.

## Qué construir

- Página **Costos** con filtros por tipo, mes y año, tabla, total y paginación.
- Modales de alta/edición y de anulación.
- Sección o modal para administrar tipos de costo.
- Servicio `costosService` para consumir la API.

## Criterios de aceptación

- [ ] La pantalla solo es visible para usuarios con rol `ADMINISTRADOR`.
- [ ] La tabla muestra fecha de pago, tipo, descripción, período, monto, estado y acciones.
- [ ] Los filtros por tipo, mes y año se aplican al listado y al total.
- [ ] Se muestra el total del filtro y se aclara que no incluye anulados.
- [ ] El botón **Nuevo costo** abre un modal con tipo, descripción, monto, fecha de pago, mes,
      año y observaciones.
- [ ] El selector de tipo solo muestra tipos activos y no reservados.
- [ ] La edición no permite cambiar el usuario de alta y está deshabilitada para costos anulados o
      generados por un ingreso.
- [ ] La anulación pide un motivo obligatorio y muestra el usuario y la fecha de anulación.
- [ ] Se pueden crear, editar y activar/desactivar tipos de costo desde la pantalla.
- [ ] Los errores de la API se muestran en banners y los campos inválidos se marcan en el
      formulario.
- [ ] Las claves usadas son `id_costo`, `id_tipo_costo` y `camelCase` para el resto
      (`fechaPago`, `periodoMes`, `periodoAnio`, `nombreTipoCosto`, `motivoAnulacion`, etc.).

## Endpoints que consume

| Método | Ruta |
|---|---|
| `GET` | `/api/costos` |
| `GET` | `/api/costos/total` |
| `POST` | `/api/costos` |
| `PUT` | `/api/costos/{id_costo}` |
| `PATCH` | `/api/costos/{id_costo}/anulacion` |
| `GET` | `/api/tipos-costo` |
| `POST` | `/api/tipos-costo` |
| `PUT` | `/api/tipos-costo/{id_tipo_costo}` |
| `PATCH` | `/api/tipos-costo/{id_tipo_costo}/estado` |

## Notas

- Todavía no existe una página de costos en `frontend/src/pages`; este ticket es el que la crea.

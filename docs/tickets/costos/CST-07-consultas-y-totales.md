# CST-07. Consultas, filtros y totales

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | `CST-03` |
| Bloquea | `CST-09`, `CST-10`, `CST-11` |
| Etiquetas | `costos`, `backend` |

**Historia:** como administrador, quiero consultar los costos por tipo y período y ver el total del
filtro para analizar los gastos del taller.

## Qué construir

- `GET /api/costos` paginado y con filtros.
- `GET /api/costos/total` con la suma del filtro.
- `GET /api/costos/{id_costo}`.

## Criterios de aceptación

- [x] `GET /api/costos` acepta `id_tipo_costo`, `mes`, `anio`, `incluirAnulados` y paginación.
- [x] Por defecto excluye los costos anulados y ordena por `fechaPago` descendente.
- [x] La respuesta es una página con `content`, `totalPages` y `totalElements`.
- [x] `GET /api/costos/total` suma los montos del mismo filtro y nunca incluye anulados.
- [x] Sin coincidencias, el total devuelve `0`.
- [x] `GET /api/costos/{id_costo}` devuelve el costo aunque esté anulado.
- [x] Un id inexistente responde 404.
- [x] Los ids de la respuesta son `id_costo`, `id_tipo_costo`, `id_usuario`,
      `id_usuario_anulacion` e `id_ingreso_mercaderia`; el resto es `camelCase`.
- [x] Solo el rol `ADMINISTRADOR` puede consultar (401 sin sesión, 403 con vendedor).

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/costos` | Página de costos filtrada |
| `GET` | `/api/costos/total` | Suma de los costos del filtro |
| `GET` | `/api/costos/{id_costo}` | Obtiene un costo por id |

## Tablas

`Costo`, `TipoCosto`.

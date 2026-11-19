# CST-02. CRUD de tipos de costo

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | `CST-01` |
| Bloquea | `CST-03`, `CST-11` |
| Etiquetas | `costos`, `backend` |

**Historia:** como administrador, quiero administrar los tipos de costo para clasificar los gastos
del taller sin depender de una lista fija.

## Qué construir

- `GET`, `POST`, `PUT` y `PATCH` bajo `/api/tipos-costo`.
- DTO de alta, edición, estado y respuesta.

## Criterios de aceptación

- [x] `GET /api/tipos-costo` devuelve los tipos activos ordenados por nombre; con
      `incluirInactivos=true` incluye los desactivados.
- [x] `POST /api/tipos-costo` crea un tipo activo y no reservado, y responde 201.
- [x] `PUT /api/tipos-costo/{id_tipo_costo}` modifica el nombre y responde 200.
- [x] `PATCH /api/tipos-costo/{id_tipo_costo}/estado` activa o desactiva el tipo.
- [x] El nombre se normaliza (sin mayúsculas, acentos ni espacios de más) para detectar
      duplicados.
- [x] Un nombre repetido responde 409.
- [x] Un id inexistente responde 404.
- [x] El tipo `reservado` (Insumos) no se puede modificar ni cambiar de estado (409).
- [x] Falta de nombre o de `activo` responde 400.
- [x] Solo el rol `ADMINISTRADOR` puede usar estos endpoints (401 sin sesión, 403 con vendedor).

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/tipos-costo` | Lista los tipos de costo |
| `POST` | `/api/tipos-costo` | Crea un tipo de costo |
| `PUT` | `/api/tipos-costo/{id_tipo_costo}` | Modifica un tipo de costo |
| `PATCH` | `/api/tipos-costo/{id_tipo_costo}/estado` | Activa o desactiva un tipo |

## Tablas

`TipoCosto`.

# CLI-04. Búsqueda y clientes similares

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | `CLI-01` |
| Bloquea | `CLI-06`, `CLI-07`, `CLI-08` |
| Etiquetas | `clientes`, `backend`, `frontend` |

**Historia:** como administrador o vendedor, quiero buscar clientes por nombre o CUIT y recibir un
aviso cuando estoy por cargar uno que ya existe con un nombre parecido.

## Qué construir

- `GET /api/clientes` paginado y con búsqueda.
- `GET /api/clientes/similares?nombre=...`.
- Buscador y aviso de similares en la pantalla de clientes.

## Criterios de aceptación

- [x] `GET /api/clientes` acepta `page`, `size`, `sort` (por defecto `nombre`), `busqueda` e
      `incluirInactivos`.
- [x] La búsqueda es parcial y case-insensitive sobre el nombre.
- [x] Si el texto parece un CUIT (solo números, espacios, puntos o guiones), se compara contra el
      CUIT sin guiones.
- [x] Por defecto solo devuelve clientes activos; con `incluirInactivos=true` incluye los inactivos.
- [x] La respuesta es una página con `content`, `totalPages` y `totalElements`.
- [x] `GET /api/clientes/similares` normaliza el texto (sin mayúsculas, acentos ni espacios de
      más), incluye inactivos, excluye al genérico y devuelve hasta 5 coincidencias.
- [x] Con menos de 3 caracteres devuelve una lista vacía.
- [x] Al escribir el nombre en el formulario de alta, el frontend consulta los similares y ofrece
      abrir el cliente existente en lugar de crear uno nuevo.
- [x] El listado muestra el cliente inactivo atenuado y con el badge **Inactivo**.

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/clientes` | Página de clientes filtrada por texto y estado |
| `GET` | `/api/clientes/similares?nombre=` | Hasta 5 clientes con nombre parecido |

## Tablas

`Cliente`.

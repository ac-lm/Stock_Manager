# USU-05. Listar y buscar usuarios

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | `USU-04` |
| Bloquea | `USU-06` a `USU-09` |
| Etiquetas | `usuario`, `backend`, `frontend` |

**Historia:** como administrador, quiero ver y buscar usuarios para encontrar una cuenta y
gestionarla.

## Qué construir

- `GET /api/usuarios` paginado y con búsqueda.
- Pantalla **Usuarios** con tabla, buscador, filtro de inactivos y paginación.

## Criterios de aceptación

- [x] `GET /api/usuarios` acepta `page`, `size`, `sort` (por defecto `nombre`), `busqueda` e
      `incluirInactivos`.
- [x] La búsqueda es parcial y case-insensitive sobre `nombre` y `email`.
- [x] Por defecto solo devuelve usuarios activos; con `incluirInactivos=true` incluye los inactivos.
- [x] La respuesta es una página con `content`, `totalPages` y `totalElements`.
- [x] La ruta requiere rol `ADMINISTRADOR`.
- [x] La tabla muestra nombre, email, rol, estado y acciones; los inactivos se ven atenuados y con
      el badge **Inactivo**.
- [x] El buscador espera 300 ms desde la última tecla y vuelve a la página 0.
- [x] El checkbox **Mostrar inactivos** recarga el listado desde la página 0.
- [x] La paginación muestra página actual, total de páginas y cantidad de usuarios.
- [x] Sin resultados se muestra `"No se encontraron usuarios."`.

## Tablas

`Usuario` (consulta, sin cambios de esquema).

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/usuarios` | Lista paginada con búsqueda y filtro de inactivos |

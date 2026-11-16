# USU-01. Consulta de usuario para otros módulos

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | — |
| Bloquea | `USU-02` a `USU-09`, `INC-02`, `INC-04`, `STOCK-01`, `VENTA` |
| Etiquetas | `usuario`, `backend`, `db` |

**Historia:** como módulo consumidor (incidencias, ventas, stock), quiero consultar los datos
públicos de un usuario por id para asociarlo sin acceder directamente a su repositorio.

## Qué construir

El modelo base del usuario y un punto de consulta reutilizable para el resto de los módulos:

- Entidad `Usuario` con tabla `Usuario`.
- Enum `Rol` con `ADMINISTRADOR` y `VENDEDOR`.
- Repositorio con búsqueda por email (ignorando mayúsculas).
- `UsuarioResponseDTO` sin contraseña.
- `UsuarioService.obtenerPorId(Long id_usuario)` y `GET /api/usuarios/{id_usuario}`.

## Criterios de aceptación

- [x] La tabla `Usuario` tiene `id_usuario` (PK), `nombre` (100), `email` (255, único),
      `password` (255), `rol` (20) y `activo` (boolean, por defecto `true`).
- [x] El rol se guarda como texto (`@Enumerated(STRING)`), no como ordinal.
- [x] `UsuarioResponseDTO` devuelve `id_usuario`, `nombre`, `email`, `rol` y `activo`, y nunca
      incluye `password`.
- [x] `GET /api/usuarios/{id_usuario}` devuelve 200 con el usuario o 404 con `ErrorResponse`.
- [x] Los emails se guardan en minúsculas y sin espacios de los bordes; la comparación es
      case-insensitive.
- [x] La baja es lógica: no se borra la fila, se usa `activo`.
- [x] Otros módulos pueden inyectar `UsuarioService` sin tocar `UsuarioRepository`.

## Tablas

`Usuario`.

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/usuarios/{id_usuario}` | Devuelve un usuario por id, incluso inactivo |

> Cuando se implementa `USU-04`, esta ruta pasa a requerir rol `ADMINISTRADOR`. Los módulos
> internos usan el servicio, no el endpoint.

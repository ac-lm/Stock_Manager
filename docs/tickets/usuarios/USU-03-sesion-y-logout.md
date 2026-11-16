# USU-03. Mantener la sesión y cerrar sesión

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | `USU-02` |
| Bloquea | `USU-04`, todas las pantallas autenticadas |
| Etiquetas | `usuario`, `backend`, `frontend` |

**Historia:** como usuario, quiero que el sistema recuerde mi sesión al recargar la página y
poder cerrarla cuando termino, para no reingresar todo el tiempo y salir de forma segura.

## Qué construir

- `GET /api/auth/me` para restaurar la sesión.
- `POST /api/auth/logout` para invalidarla.
- Contexto de autenticación en el frontend.
- Datos del usuario y botón de cierre en la barra de navegación.

## Criterios de aceptación

- [x] `GET /api/auth/me` devuelve 200 con el usuario de la sesión o 401 con `ErrorResponse`.
- [x] `POST /api/auth/logout` invalida la sesión, borra la cookie y responde 204.
- [x] El timeout de sesión está configurado en 8 horas.
- [x] Al cargar la aplicación, el frontend consulta `/auth/me` y restaura el usuario.
- [x] Mientras verifica la sesión se muestra `"Verificando sesión..."` y no parpadea el login.
- [x] Un 401 en cualquier petición autenticada limpia el usuario local y redirige a `/login`
      (salvo en el login y en la consulta inicial de sesión, donde el 401 es esperado).
- [x] La barra de navegación muestra el nombre y el rol del usuario y un botón
      **Cerrar sesión**.
- [x] Al cerrar sesión se navega a `/login` y `/auth/me` vuelve a responder 401.

## Tablas

Ninguna: la sesión vive en Tomcat. Reiniciar el backend cierra todas las sesiones.

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/auth/me` | Devuelve el usuario de la sesión actual |
| `POST` | `/api/auth/logout` | Invalida la sesión actual |

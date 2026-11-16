# USU-09. Activar y desactivar usuarios

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | `USU-06` |
| Bloquea | `USU-10` |
| Etiquetas | `usuario`, `backend`, `frontend` |

**Historia:** como administrador, quiero dar de baja lógica a un usuario para que no pueda entrar
sin perder su historial, y poder reactivarlo si hace falta.

## Qué construir

- `PATCH /api/usuarios/{id_usuario}/estado`.
- Acciones **Desactivar** / **Reactivar** en la pantalla de administración.

## Criterios de aceptación

- [x] El body pide `{ "activo": true | false }`; si falta, responde 400.
- [x] Desactivar es una baja lógica: la fila no se borra y conserva su historial.
- [x] Un usuario inactivo no puede iniciar sesión: el login responde 401 con
      `"La cuenta está deshabilitada"`.
- [x] No se puede desactivar la cuenta con la que se está autenticado: responde 409 con
      `"No se puede desactivar el usuario con el que se está autenticado"`.
- [x] No se puede dejar el sistema sin administradores activos: responde 409 con
      `"Debe quedar al menos un administrador activo"`.
- [x] Reactivar vuelve a permitir el login.
- [x] La respuesta es 200 con el usuario actualizado, sin contraseña.
- [x] Un id inexistente responde 404 y un vendedor recibe 403.
- [x] La tabla muestra **Desactivar** para activos y **Reactivar** para inactivos.
- [x] Desactivar pide confirmación porque el usuario dejará de poder entrar.
- [x] El botón **Desactivar** de la propia cuenta aparece deshabilitado y explica el motivo.
- [x] Los inactivos solo se ven con **Mostrar inactivos** y se muestran atenuados.

## Tablas

`Usuario`.

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| `PATCH` | `/api/usuarios/{id_usuario}/estado` | Activa o desactiva un usuario |

# USU-08. Cambiar la contraseña de un usuario

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | `USU-06` |
| Bloquea | `USU-10` |
| Etiquetas | `usuario`, `backend`, `frontend` |

**Historia:** como administrador, quiero asignar una nueva contraseña a un usuario para ayudarlo
cuando la olvida o para rotarla.

## Qué construir

- `PUT /api/usuarios/{id_usuario}/password`.
- Acción **Contraseña** y modal con confirmación en la pantalla de administración.

## Criterios de aceptación

- [x] El body pide `password`, con mínimo de 8 caracteres y máximo de 72 bytes (límite de BCrypt).
- [x] Una contraseña inválida responde 400 con el mensaje correspondiente.
- [x] La contraseña se cifra con BCrypt y nunca se devuelve.
- [x] Una actualización correcta responde 204 sin cuerpo.
- [x] Un id inexistente responde 404.
- [x] Solo un administrador puede cambiar contraseñas: un vendedor recibe 403.
- [x] El modal pide la nueva contraseña y su repetición, y valida en el frontend que coincidan.
- [x] Al guardar se muestra el mensaje de éxito y se cierra el modal.
- [x] Después del cambio, la contraseña anterior deja de funcionar y la nueva permite iniciar
      sesión.

## Tablas

`Usuario`.

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| `PUT` | `/api/usuarios/{id_usuario}/password` | Asigna una nueva contraseña |

## Notas

- Cambiar la contraseña no invalida las sesiones ya abiertas de ese usuario; seguirán activas hasta
  su timeout o hasta que cierre sesión.
- El autocambio de contraseña por parte del usuario no está incluido; este ticket es la vía
  administrativa.

# USU-07. Editar un usuario

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | `USU-06` |
| Bloquea | `USU-10` |
| Etiquetas | `usuario`, `backend`, `frontend` |

**Historia:** como administrador, quiero corregir el nombre, el email o el rol de una cuenta para
mantenerla actualizada.

## Qué construir

- `PUT /api/usuarios/{id_usuario}` con actualización parcial.
- Modal **Editar usuario** en la pantalla de administración.

## Criterios de aceptación

- [x] El body acepta `nombre`, `email` y `rol`; los campos nulos o vacíos no se modifican.
- [x] El email se normaliza a minúsculas y se valida como único excluyendo al propio usuario.
- [x] Un email repetido responde 409 con `"Ya existe un usuario con ese email"`.
- [x] Si se cambia el rol, se valida que quede al menos un administrador activo; si no, responde
      409 con `"Debe quedar al menos un administrador activo"`.
- [x] Un id inexistente responde 404 y un email inválido responde 400.
- [x] La respuesta es 200 con el usuario actualizado, sin contraseña.
- [x] El modal **Editar usuario** viene precargado con nombre, email y rol.
- [x] Al guardar se muestra el mensaje de éxito y se recarga el listado.
- [x] Si el administrador edita su propia cuenta, se refresca la sesión para actualizar el nombre
      y el rol de la barra de navegación.
- [x] Si cambia su propio email, se avisa que debe volver a iniciar sesión y la aplicación lo
      redirige al login al no poder resolver la sesión con el email anterior.

## Tablas

`Usuario`.

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| `PUT` | `/api/usuarios/{id_usuario}` | Actualiza parcialmente un usuario |

## Notas

- La sesión guarda el email con el que se inició sesión. Cambiar el email propio deja la sesión
  anterior inconsistente hasta el próximo login; por eso el frontend avisa y refresca la sesión.

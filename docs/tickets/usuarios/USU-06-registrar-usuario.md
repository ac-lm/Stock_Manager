# USU-06. Registrar un usuario

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | `USU-05` |
| Bloquea | `USU-07`, `USU-08`, `USU-09` |
| Etiquetas | `usuario`, `backend`, `frontend` |

**Historia:** como administrador, quiero dar de alta usuarios con su rol y una contraseña inicial
para que puedan acceder al sistema.

## Qué construir

- `POST /api/usuarios` con validaciones y email único.
- Modal **Nuevo usuario** en la pantalla de administración.

## Criterios de aceptación

- [x] El body pide `nombre`, `email`, `password` y `rol`; todos son obligatorios.
- [x] Se valida nombre de hasta 100 caracteres, email válido de hasta 255, contraseña de al menos
      8 caracteres y hasta 72 bytes, y rol dentro de `ADMINISTRADOR` / `VENDEDOR`.
- [x] El email se guarda en minúsculas y debe ser único, comparando sin distinguir mayúsculas.
- [x] Un email repetido responde 409 con `"Ya existe un usuario con ese email"`.
- [x] La contraseña se cifra con BCrypt y el usuario se crea con `activo = true`.
- [x] La respuesta es 201 con `UsuarioResponseDTO`, sin contraseña.
- [x] Los errores de validación responden 400 con `"Hay errores de validación"` y el detalle por
      campo.
- [x] El modal **Nuevo usuario** pide nombre, email, rol y contraseña inicial, con mínimo de 8
      caracteres.
- [x] Al guardar se muestra el mensaje de éxito y se recarga el listado.
- [x] Un vendedor no puede llamar al endpoint: recibe 403.

## Tablas

`Usuario`.

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/usuarios` | Crea un usuario activo y devuelve 201 |

# USU-02. Iniciar sesión con email y contraseña

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | `USU-01` |
| Bloquea | `USU-03` a `USU-09` |
| Etiquetas | `usuario`, `backend`, `frontend`, `db` |

**Historia:** como usuario del taller, quiero iniciar sesión con mi email y contraseña para
acceder al sistema con mi rol.

## Qué construir

Autenticación con sesión HTTP y una pantalla de login:

- `UsuarioUserDetailsService` que carga al usuario por email.
- `AuthenticationManager` con BCrypt.
- `POST /api/auth/login`, que crea la sesión y devuelve los datos del usuario.
- Administrador inicial automático para poder entrar la primera vez.
- Pantalla `/login` en el frontend.

## Criterios de aceptación

- [x] La contraseña se guarda cifrada con BCrypt y nunca se devuelve en una respuesta.
- [x] `POST /api/auth/login` recibe `email` y `password`; el email se limpia y pasa a minúsculas.
- [x] Credenciales incorrectas responden 401 con `"Email o contraseña incorrectos"`.
- [x] Una cuenta inactiva responde 401 con `"La cuenta está deshabilitada"`.
- [x] Datos inválidos (email mal formado o campos vacíos) responden 400 con el detalle.
- [x] La sesión se guarda en una cookie `JSESSIONID` `HttpOnly`, `SameSite=Lax`, con 8 horas de
      timeout.
- [x] Si ya existía una sesión, se renueva el id antes de autenticar (protección contra fijación
      de sesión).
- [x] Si la tabla `Usuario` está vacía, al arrancar se crea `admin@taller.local` / `Admin1234`,
      configurable con `APP_ADMIN_EMAIL` y `APP_ADMIN_PASSWORD`; si ya hay usuarios, no hace nada.
- [x] La pantalla `/login` pide email y contraseña, muestra el error del backend, deshabilita el
      botón mientras envía y redirige al origen o a `/insumos`.
- [x] Las credenciales iniciales solo se muestran como ayuda en desarrollo.
- [x] La cookie funciona a través del proxy de Vite porque el cliente envía
      `credentials: 'include'`.

## Tablas

`Usuario` (sin cambios de esquema respecto de `USU-01`).

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/auth/login` | Valida credenciales y crea la sesión |

## Notas

- El usuario inicial solo se crea si la tabla está vacía. Si existe una tabla vieja con otro
  esquema, conviene recrearla para que el inicializador corra.
- La recuperación de contraseña olvidada no forma parte de este ticket.

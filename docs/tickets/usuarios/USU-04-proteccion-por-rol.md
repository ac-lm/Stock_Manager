# USU-04. Proteger la API y las rutas por rol

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | `USU-02`, `USU-03` |
| Bloquea | `USU-05` a `USU-09` |
| Etiquetas | `usuario`, `backend`, `frontend` |

**Historia:** como administrador, quiero que solo mi rol pueda gestionar usuarios, para que un
vendedor no pueda crear, editar ni desactivar cuentas.

## Qué construir

- Reglas de autorización en Spring Security.
- Respuestas JSON uniformes para 401 y 403.
- Guardas de ruta y navegación condicional en el frontend.

## Criterios de aceptación

- [x] `/api/auth/login` es público; el resto de `/api/auth/**` requiere sesión.
- [x] `/api/usuarios/**` requiere rol `ADMINISTRADOR`.
- [x] Sin sesión, la API responde 401 con `"Debe iniciar sesión para acceder a este recurso"`.
- [x] Con sesión pero sin permisos, responde 403 con
      `"No tiene permisos para realizar esta operación"`.
- [x] Swagger (`/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**`), `/api/auth/login` y
      `/error` son públicos.
- [x] `formLogin`, `httpBasic` y el logout de Spring quedan deshabilitados; el login y el logout
      se manejan por los endpoints propios.
- [x] La política de sesión es `IF_REQUIRED` y CSRF está deshabilitado porque el frontend consume
      la API desde el mismo origen.
- [x] El resto de la API sigue en `permitAll()` de forma temporal, con un TODO para pasar a
      `anyRequest().authenticated()` cuando todas las pantallas tengan login.
- [x] El frontend redirige a `/login` si no hay sesión, guardando la ruta de origen.
- [x] Un usuario `VENDEDOR` que entra a `/usuarios` es redirigido a `/insumos`.
- [x] El link **Usuarios** de la barra solo se muestra a los administradores.
- [x] La protección visual no reemplaza la del backend: un vendedor que llama directo a la API
      recibe 403.

## Tablas

Ninguna.

## Endpoints

Todos los de `/api/usuarios/**` y `/api/auth/**`.

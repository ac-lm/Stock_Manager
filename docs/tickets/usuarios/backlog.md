# Backlog — Módulo Usuario

Tickets del módulo `usuario` escritos como rebanadas verticales: cada uno incluye base de datos,
backend y pantalla cuando corresponde, y al terminarlo se puede probar de punta a punta. Están
en orden; **Bloqueado por** indica qué tiene que estar terminado antes.

**Decisiones vigentes:** login con **email y contraseña**, sesión HTTP con cookie `HttpOnly`
(no JWT), contraseñas cifradas con BCrypt, baja lógica con `activo` y dos roles:
`ADMINISTRADOR` y `VENDEDOR`. La sesión dura 8 horas.

> El documento [`10-usuarios-auditoria.md`](10-usuarios-auditoria.md) quedó como registro de
> preguntas y menciona JWT/username; este backlog refleja lo que efectivamente se implementó.

Leyenda de estado: ✅ Hecho · 🟡 En curso · 🟢 Listo · 🔴 Bloqueado.

## Resumen

| ID | Ticket | Bloqueado por | Estado |
|---|---|---|---|
| [USU-01](USU-01-consulta-usuario.md) | Consulta de usuario para otros módulos | — | ✅ |
| [USU-02](USU-02-iniciar-sesion.md) | Iniciar sesión con email y contraseña | 01 | ✅ |
| [USU-03](USU-03-sesion-y-logout.md) | Mantener la sesión y cerrar sesión | 02 | ✅ |
| [USU-04](USU-04-proteccion-por-rol.md) | Proteger la API y las rutas por rol | 02, 03 | ✅ |
| [USU-05](USU-05-listar-usuarios.md) | Listar y buscar usuarios | 04 | ✅ |
| [USU-06](USU-06-registrar-usuario.md) | Registrar un usuario | 05 | ✅ |
| [USU-07](USU-07-editar-usuario.md) | Editar un usuario | 06 | ✅ |
| [USU-08](USU-08-cambiar-password.md) | Cambiar la contraseña de un usuario | 06 | ✅ |
| [USU-09](USU-09-activar-desactivar-usuario.md) | Activar y desactivar usuarios | 06 | ✅ |
| [USU-10](USU-10-documentacion-swagger.md) | Documentar la API en Swagger | 07, 08, 09 | ✅ |

## Orden recomendado

1. `USU-01`.
2. `USU-02`.
3. **En paralelo:** `USU-03` y, cuando esté, `USU-04`.
4. `USU-05`.
5. `USU-06`.
6. **En paralelo:** `USU-07`, `USU-08` y `USU-09`.
7. `USU-10`.

**Camino crítico:** `USU-01 → USU-02 → USU-03 → USU-04 → USU-05 → USU-06 → USU-07 → USU-10`.

## Prerrequisitos de otros módulos

- `common`: formato de errores `ErrorResponse` y manejo global de excepciones.
- PostgreSQL con la tabla `Usuario` (se crea/actualiza con Hibernate).
- Frontend con React Router y el proxy de Vite `/api` → `http://localhost:8080`.
- Para probar el resto de la API con sesión hace falta que los demás módulos también exijan login;
  por ahora solo `/api/usuarios/**` está protegido (ver `USU-04`).

## Verificación del módulo

```bash
cd backend
./mvnw clean package -DskipTests

cd ../frontend
npm run lint
npm run build
```

Prueba manual en una base limpia:

1. Arrancar backend y frontend.
2. Entrar con `admin@taller.local` / `Admin1234`.
3. Crear un vendedor, editarle el nombre, cambiarle la contraseña y desactivarlo.
4. Verificar que el vendedor no ve el link **Usuarios** ni puede entrar a `/usuarios`.
5. Verificar que una cuenta desactivada no puede iniciar sesión y que no se puede desactivar el
   último administrador ni la propia cuenta.

## Fuera de alcance

- Recuperación de contraseña por email (se acordó en las preguntas, pero no se implementó).
- Autocambio de contraseña por parte del usuario: el cambio es administrativo.
- Registro público de usuarios: solo un administrador da de alta cuentas.
- Auditoría de login y de cambios de usuario (corresponde al módulo `auditoria`).
- Tests automatizados permanentes: la implementación se validó manualmente contra PostgreSQL.
- Protección del resto de la API (`anyRequest().authenticated()`): queda como TODO hasta que
  todas las pantallas tengan login.

# USU-10. Documentar la API en Swagger

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | `USU-07`, `USU-08`, `USU-09` |
| Bloquea | — |
| Etiquetas | `usuario`, `backend`, `docs` |

**Historia:** como desarrollador o integrador, quiero ver los endpoints del módulo documentados en
Swagger para probarlos y entenderlos sin leer el código.

## Qué construir

- Anotaciones OpenAPI en la interfaz `UsuarioApi` y en `AuthController`.
- Swagger UI público para consultar y ejecutar los endpoints.

## Criterios de aceptación

- [x] `/swagger-ui.html` y `/v3/api-docs` son públicos.
- [x] Hay dos grupos visibles: **Autenticación** y **Usuarios**.
- [x] Cada operación tiene resumen y descripción.
- [x] Cada operación documenta los códigos que puede devolver: 200, 201, 204, 400, 401, 403, 404
      y 409 según corresponda.
- [x] Los DTO de entrada documentan ejemplos y los roles permitidos.
- [x] El campo de contraseña se marca como obligatorio y no se expone en las respuestas.
- [x] Se puede hacer login desde Swagger y luego probar el CRUD usando la misma cookie de sesión.
- [x] La documentación refleja la implementación real: sesión HTTP por email, no JWT ni username.

## Tablas

Ninguna.

## Endpoints

Todos los de `/api/auth/**` y `/api/usuarios/**`.

## Notas

- En producción conviene desactivar Swagger con `springdoc.api-docs.enabled=false` y
  `springdoc.swagger-ui.enabled=false`, o restringirlo a administradores.

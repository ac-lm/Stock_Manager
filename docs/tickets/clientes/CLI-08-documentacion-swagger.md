# CLI-08. Documentar la API en Swagger

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | `CLI-03`, `CLI-04`, `CLI-05` |
| Bloquea | — |
| Etiquetas | `clientes`, `backend`, `docs` |

**Historia:** como desarrollador o integrador, quiero ver los endpoints de clientes documentados en
Swagger para probarlos y entenderlos sin leer el código.

## Qué construir

- Anotaciones OpenAPI en `ClienteApi` y en los DTO de clientes.
- Grupo **Clientes** visible en Swagger UI.

## Criterios de aceptación

- [x] `/swagger-ui.html` y `/v3/api-docs` son públicos.
- [x] El grupo **Clientes** muestra las 6 operaciones: listar, obtener, crear, actualizar, cambiar
      estado y similares.
- [x] Cada operación tiene resumen y descripción.
- [x] Cada operación documenta los códigos que puede devolver: 200, 201, 400, 404 y 409 según
      corresponda.
- [x] Los parámetros de ruta se documentan como `id_cliente`.
- [x] Los DTO de entrada y salida documentan ejemplos, campos obligatorios y valores permitidos de
      `tipoCliente`.
- [x] Los errores se documentan con el esquema `ErrorResponse`.
- [x] Se puede probar el alta y la búsqueda desde Swagger UI.

## Archivos

- `backend/src/main/java/com/taller/gestion/clientes/controller/ClienteApi.java`
- `backend/src/main/java/com/taller/gestion/clientes/dto/ClienteCreateDTO.java`
- `backend/src/main/java/com/taller/gestion/clientes/dto/ClienteUpdateDTO.java`
- `backend/src/main/java/com/taller/gestion/clientes/dto/ClienteResponseDTO.java`
- `backend/src/main/java/com/taller/gestion/clientes/dto/ClienteEstadoDTO.java`
- `backend/src/main/java/com/taller/gestion/clientes/dto/ClienteSimilarDTO.java`

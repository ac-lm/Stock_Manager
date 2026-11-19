# CST-10. Documentar la API en Swagger

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | `CST-03`, `CST-04`, `CST-07` |
| Bloquea | — |
| Etiquetas | `costos`, `backend`, `docs` |

**Historia:** como desarrollador o integrador, quiero ver los endpoints de costos y tipos de costo
documentados en Swagger para probarlos y entenderlos sin leer el código.

## Qué construir

- Anotaciones OpenAPI en `CostoApi` y `TipoCostoApi`.
- Documentación de todos los DTO de costos con `@Schema`.
- Grupos **Costos** y **Tipos de costo** en Swagger UI.

## Criterios de aceptación

- [x] El grupo **Costos** documenta las 6 operaciones: listar, total, obtener, crear, actualizar y
      anular.
- [x] El grupo **Tipos de costo** documenta las 4 operaciones: listar, crear, actualizar y cambiar
      estado.
- [x] Cada operación tiene resumen y descripción.
- [x] Cada operación documenta los códigos que puede devolver: 200, 201, 400, 401, 403, 404 y 409
      según corresponda.
- [x] Los parámetros de ruta se documentan como `id_costo` e `id_tipo_costo`.
- [x] Los 8 DTO de costos documentan ejemplos, campos obligatorios y valores permitidos.
- [x] Se aclara que el usuario de alta y el de anulación salen de la sesión.
- [x] Los errores se documentan con el esquema `ErrorResponse`.
- [x] Se puede probar el alta y el filtro de costos desde Swagger UI con una sesión de
      administrador.

## Archivos

- `backend/src/main/java/com/taller/gestion/costos/controller/CostoApi.java`
- `backend/src/main/java/com/taller/gestion/costos/controller/TipoCostoApi.java`
- `backend/src/main/java/com/taller/gestion/costos/dto/*.java`

## Notas

- El estado ✅ es para el alcance actual. Cuando se conecten `CST-05`, `CST-06` y `CST-08` habrá
  que documentar los endpoints y campos nuevos.

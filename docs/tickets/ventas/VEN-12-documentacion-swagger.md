# VEN-12. Documentar la API en Swagger

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho (parcial: actualizar con los tickets diferidos) |
| Bloqueado por | `VEN-03`, `VEN-04`, `VEN-05` |
| Bloquea | — |
| Etiquetas | `ventas`, `docs` |

**Historia:** como frontend, quiero ver en Swagger los endpoints de ventas y sus respuestas.

## Qué construir

- Anotaciones `@Tag`, `@Operation`, `@ApiResponse` en `VentaController`.
- `@Schema` en los DTOs de request y response.

## Criterios de aceptación

- [x] Tag **Ventas** con descripción del alcance (sin stock ni comprobantes).
- [x] Endpoints documentados con sus códigos (201, 200, 400, 404, 409).
- [x] DTOs con ejemplos y campos opcionales marcados.
- [ ] Documentar remito, factura y pedidos cuando se implementen.

## Verificación

Abrir `/swagger-ui.html` con el backend corriendo y probar el alta desde la interfaz.

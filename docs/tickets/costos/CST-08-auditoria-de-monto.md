# CST-08. Auditar cambios de monto

| Campo | Valor |
|---|---|
| Estado | 🟢 Listo |
| Bloqueado por | `CST-04` |
| Bloquea | — |
| Etiquetas | `costos`, `backend`, `auditoria` |

**Historia:** como administrador, quiero saber cuándo y cuánto cambió el monto de un costo para
detectar correcciones o errores de carga.

## Qué construir

- Registro de auditoría del monto anterior y el nuevo al editar un costo.
- Reutilizar el mecanismo del módulo `auditoria` o, si no existe, dejar el evento preparado.

## Criterios de aceptación

- [ ] Al modificar el monto de un costo se registra el valor anterior y el nuevo.
- [ ] La auditoría incluye el usuario que hizo el cambio y la fecha y hora.
- [ ] Si el monto no cambia, no se genera un registro de auditoría de monto.
- [ ] La edición sigue funcionando igual para el cliente de la API.
- [ ] Hay tests que cubren el cambio de monto y el caso sin cambio.

## Notas

- En `CostoService.actualizar` ya existe el `TODO (CST-08)` que marca el punto de extensión.
- El módulo `auditoria` es transversal; si todavía no existe, este ticket puede implementar el
  registro dentro de `costos` y migrarlo después.

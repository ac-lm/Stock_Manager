# CST-05. Integración con caja

| Campo | Valor |
|---|---|
| Estado | 🔴 Bloqueado |
| Bloqueado por | `CST-04`, módulo `caja` |
| Bloquea | — |
| Etiquetas | `costos`, `caja`, `backend` |

**Historia:** como administrador, quiero que un costo pagado con plata de la caja no se pueda anular
por separado del egreso de caja, para que la caja y los costos no queden descuadrados.

## Qué construir

- Vínculo `id_costo` en los egresos de caja (lo aporta el módulo `caja`).
- Validación en `CostoService.anular` para rechazar costos con egreso asociado.

## Criterios de aceptación

- [ ] Un egreso de caja puede referenciar un costo mediante `id_costo`.
- [ ] Al anular un costo vinculado a un egreso, la API responde 409 con un mensaje que indica que
      primero se anule el egreso.
- [ ] El mensaje sugiere la acción concreta: `"anulá primero el egreso"`.
- [ ] La validación no rompe la anulación de costos sin vínculo.
- [ ] Hay tests de servicio y de controller para el caso vinculado y el no vinculado.

## Notas

- En `CostoService.anular` ya existe el `TODO (CST-05)` que marca dónde va la validación.
- Este ticket depende de que el módulo `caja` exista y exponga la consulta de egresos por costo.

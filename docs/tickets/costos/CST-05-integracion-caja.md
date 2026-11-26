# CST-05. Integración con caja

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | `CST-04`, módulo `caja` |
| Bloquea | — |
| Etiquetas | `costos`, `caja`, `backend` |

**Historia:** como administrador, quiero que un costo pagado con plata de la caja no se pueda anular
por separado del egreso de caja, para que la caja y los costos no queden descuadrados.

## Qué construir

- Vínculo `id_costo` en los egresos de caja (lo aporta el módulo `caja`).
- Validación en `CostoService.anular` para rechazar costos con egreso asociado.

## Criterios de aceptación

- [x] Un egreso de caja puede referenciar un costo mediante `id_costo` (campo opcional en
      `MovimientoCajaCreateDTO`, guardado por `CajaService.registrarEgreso`).
- [x] Al anular un costo vinculado a un egreso, la API responde 409 con un mensaje que indica que
      primero se anule el egreso.
- [x] El mensaje sugiere la acción concreta: `"anulá primero el egreso"`.
- [x] La validación no rompe la anulación de costos sin vínculo.
- [x] Hay tests de servicio y de controller para el caso vinculado y el no vinculado, más un test
      de repositorio de la consulta por `id_costo`.

## Notas

- `CajaService.existeEgresoVinculadoACosto(id_costo)` expone la consulta; costos llama al service de
  caja, nunca a su repositorio, para no acoplar módulos.
- El vínculo se graba solo en egresos (`ConceptoMovimientoCaja.RETIRO` o `GASTO`). Cualquier
  movimiento que referencie el costo bloquea su anulación; no hay anulación de egresos en esta fase
  (ver ADR `0002-anulacion-por-contramovimiento.md`).
- El mensaje completo es: `"El costo está vinculado a un egreso de caja: anulá primero el egreso"`.

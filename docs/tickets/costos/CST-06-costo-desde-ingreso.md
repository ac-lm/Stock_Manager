# CST-06. Costo automático desde el ingreso de mercadería

| Campo | Valor |
|---|---|
| Estado | 🔴 Bloqueado |
| Bloqueado por | `CST-01`, módulo de ingresos de mercadería y stock |
| Bloquea | — |
| Etiquetas | `costos`, `stock`, `backend` |

**Historia:** como administrador, quiero que al ingresar mercadería de un proveedor se registre
automáticamente un costo de tipo Insumos por el total, para no cargar el mismo gasto dos veces.

## Qué construir

- Generación del costo al confirmar un ingreso de mercadería.
- Vínculo `id_ingreso_mercaderia` en el costo generado.
- Bloqueo de edición y anulación manual de esos costos.

## Criterios de aceptación

- [ ] Al registrar un ingreso de mercadería se crea un `Costo` con tipo Insumos, monto igual al
      total del ingreso y período correspondiente a la fecha.
- [ ] El costo generado guarda `id_ingreso_mercaderia` y queda marcado como
      `generadoPorIngreso = true` en la respuesta.
- [ ] El costo generado usa el usuario de la sesión como usuario de alta.
- [ ] Un costo generado por ingreso no se puede editar a mano (409).
- [ ] Un costo generado por ingreso no se puede anular a mano (409); se anula anulando el ingreso.
- [ ] El tipo Insumos se crea como `reservado` y no se puede usar para cargas manuales.
- [ ] Hay tests de integración que cubren la creación, la idempotencia y el bloqueo de edición y
      anulación.

## Notas

- La entidad `Costo` ya tiene `id_ingreso_mercaderia` único y `CostoService` ya rechaza editar o
  anular costos con ese vínculo.
- El tipo reservado **Insumos** ya se crea automáticamente al arrancar (ver `CST-01`), así que no
  hace falta cargarlo a mano.
- Falta el módulo que origina el ingreso de mercadería.

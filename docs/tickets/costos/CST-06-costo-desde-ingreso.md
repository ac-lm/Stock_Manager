# CST-06. Costo automático desde el ingreso de mercadería

| Campo | Valor |
|---|---|
| Estado | 🔴 Bloqueado hasta acuerdo con Lautaro |
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

## Opciones evaluadas (27/11/2026)

1. **Módulo de ingreso de mercadería (elegida).** Alta y confirmación de ingresos de proveedor con
   entidad y API propias; al confirmarse se genera el `Costo` de tipo Insumos por el total y se
   guarda `id_ingreso_mercaderia`. Permite anular el ingreso y, junto con él, el costo generado.
2. **Reutilizar `Movimiento_Stock` (`ENTRADA`/`COMPRA`).** Sin módulo nuevo: disparar el costo desde
   el movimiento de stock existente. Descartada porque el movimiento guarda cantidades, pero no
   monto, proveedor ni una unidad de ingreso anulable, así que no se puede reconstruir el total ni
   revertir el costo.
3. **Puente solo en Costos con un endpoint propio, sin tocar Stock.** Agregar un endpoint en
   `costos` para registrar el ingreso como costo de Insumos, sin crear el módulo de ingresos ni
   modificar Stock. Descartada porque el origen queda fuera del módulo que realmente mueve el
   stock: no se genera el vínculo real con `Movimiento_Stock`, no se puede anular el ingreso y el
   costo quedaría cargado "a mano" con otro nombre.

La opción 1 es la elegida, pero **no está implementada**. No se creó la entidad ni el módulo, no se
tocó Stock ni se agregó código a `CostoService`. Hoy solo existen los campos reservados
`Costo.id_ingreso_mercaderia` y `MovimientoStock.id_ingreso_mercaderia`; `generadoPorIngreso` se
calcula en `CostoService` para la respuesta, sin persistirse.

## Decisiones abiertas del cuestionario 04

A confirmar con Lautaro antes de empezar; las respuestas vigentes son las de
[`04-costos-res.md`](04-costos-res.md):

| # | Tema | Qué falta acordar |
|---|---|---|
| 6 | Sueldos: total por período con observación (a) o por persona (b) | Confirmar la opción (a) y que no amplía el alcance del módulo de ingresos |
| 7 | Evolución del costo de insumos: costo unitario por ingreso (a) o tabla de historial manual (b) | Confirmar la opción (a): define que el ingreso debe guardar precio unitario y fecha |
| 8 | Quién ve y carga costos: solo administradores (a) o también vendedores (b) | Confirmar la opción (a): define los permisos del módulo nuevo |

## Notas

- La entidad `Costo` ya tiene `id_ingreso_mercaderia` único y `CostoService` ya rechaza editar o
  anular costos con ese vínculo.
- El tipo reservado **Insumos** ya se crea automáticamente al arrancar (ver `CST-01`), así que no
  hace falta cargarlo a mano.
- Falta el módulo que origina el ingreso de mercadería.
- **Bloqueado:** requiere acuerdo con Lautaro sobre las decisiones abiertas antes de tocar Stock o
  crear el módulo de ingresos. Los tickets `INC-07` y `VEN-10` tampoco están integrados.

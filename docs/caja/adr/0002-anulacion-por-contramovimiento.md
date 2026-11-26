# 0002. Las anulaciones se registran con un contramovimiento, nunca borrando

- Estado: aceptada
- Fecha: 2026-11-04

## Contexto

El PDF exige que cada cierre quede registrado y que no se pueda modificar sin dejar trazabilidad. Si al anular una venta se borrara o editara su movimiento de caja, el total esperado de una sesión ya cerrada cambiaría después del cierre.

## Decisión

Un movimiento de caja registrado nunca se borra ni se edita. Anular una venta cambia su estado a `anulada` y crea un **egreso por el mismo monto** en la sesión abierta en ese momento, apuntando a la misma venta. Las correcciones de un cierre las hace solo un administrador y quedan en Auditoria con el valor anterior y el nuevo.

## Alternativas consideradas

- **Borrar el movimiento de la venta.** Más simple, pero cambia sesiones ya cerradas y se pierde el rastro.
- **Permitir anular solo dentro de la misma sesión.** Evita el problema, pero no cubre una devolución al día siguiente.

## Consecuencias

- Los cierres anteriores nunca se mueven.
- La devolución aparece en la sesión del día en que realmente salió el dinero.
- Los reportes de ventas tienen que filtrar por estado `confirmada` para no contar las anuladas.

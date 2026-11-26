# 0001. La caja se modela como sesión con movimientos

- Estado: aceptada
- Fecha: 2026-11-04

## Contexto

El PDF del proyecto pide registrar cierres de caja con fecha, hora, usuario, monto inicial, ventas, medios de pago, total esperado, total declarado y diferencia, con varios cierres posibles en un mismo día. El primer diagrama tenía una sola tabla de cierre. Para calcular el total esperado hace falta saber todo el dinero que entró y salió desde la apertura, y por qué medio.

## Decisión

La caja se modela como una **sesión** que se abre y se cierra (Sesion_Caja), con **movimientos de caja** (Movimiento_Caja) que cuelgan de ella, y un **detalle del cierre por medio de pago** (Cierre_Caja_Detalle). La venta no apunta a la sesión: llega a ella a través de sus movimientos. El medio de pago es un enum (efectivo, transferencia).

## Alternativas consideradas

- **Una sola tabla de cierre con los totales.** Más simple, pero el total esperado tendría que calcularse a mano o desde las ventas, y no habría lugar para retiros ni gastos pagados con la caja.
- **Venta con FK directa a la sesión.** Duplica el dato (la venta ya llega por sus movimientos) y no sirve para pagos mixtos.
- **Un único total declarado para todos los medios.** Un faltante de efectivo podría quedar tapado por una transferencia de más.

## Consecuencias

- El total esperado por medio de pago se calcula sumando los movimientos de la sesión.
- Una venta puede tener varios movimientos (pagos mixtos); la suma tiene que dar el total, y eso lo valida el backend.
- Hay que tener siempre una sesión abierta para poder vender.

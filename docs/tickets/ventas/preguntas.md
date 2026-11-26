# Módulo Ventas: preguntas (grill-me)

**Cómo completarlo:** marquen una opción (o escriban otra) en "Respuesta". ★ = la que recomiendo, con el motivo. **(clienta)** = probablemente solo la sepa ella.

**Estado:** relevamiento cerrado. Las decisiones vigentes de esta tanda están en el [backlog](backlog.md); los temas acordados pero diferidos quedan registrados acá con su ticket.

**Relacionado con:** `clientes` (pregunta 1), `catalogo` (precios y stock), `stock` (descuento), `caja` (medio de pago y seña), `pedidos`.

---

## A. Cliente de la venta

### 1. ¿Toda venta tiene que tener un cliente cargado?

- b) ★ Hay un cliente genérico "Consumidor final" para las ventas de mostrador; los grandes clientes se cargan con datos. _Motivo: no frena la venta rápida y los reportes por cliente siguen funcionando._

**Respuesta:** b) Cliente genérico "Consumidor final".

**Impacto:** `Venta.id_cliente` es obligatorio. Para mostrador se usa el cliente `GENERICO` que ya existe como tipo reservado en `clientes`. Hoy **no hay forma de obtenerlo** desde `ClienteService` ni un inicializador que lo cree: ver prerrequisitos del backlog.

### 2. (clienta) ¿Los grandes clientes tienen precios distintos?

- b) Hay una lista de precios por tipo de cliente.

**Respuesta:** b) Lista de precios por tipo de cliente. Prevalece sobre la pregunta 6 (ver aclaración 1).

**Impacto:** el precio unitario no se toma directamente de `Producto.precio`, sino de la lista correspondiente al `TipoCliente`. Requiere una tabla nueva que no está en el código actual; su estructura debe salir del DDL de Vertabelo.

---

## B. Precios y descuentos

### 3. ¿Dónde se aplica el descuento?

**Respuesta:** No hay descuento.

**Impacto:** no se persiste un campo `descuento` en `Venta`. La columna `descuento` del esqueleto actual se elimina.

### 6. ¿Se puede cambiar el precio de un producto al venderlo?

- a) ★ No, siempre es el precio de catálogo; para bajar el precio se usa un descuento. _Motivo: un solo camino para rebajar y queda registrado como descuento._

**Respuesta:** se descarta esta opción. Se aplica la aclaración 1: el precio viene de la **lista por tipo de cliente**, nadie lo edita al vender y no se guarda descuento. La opción 6a era incompatible con la 2b y con la 3.

**Impacto:** ningún campo editable de precio en el alta de venta; el precio unitario se calcula en el service.

---

## C. Productos, unidades y stock

### 7. (clienta) ¿Hay productos que se venden por unidad y también por peso?

- a) ★ No, cada producto se vende de una sola forma. _Motivo: alcanza con la unidad de venta del producto; no hace falta otra tabla._

**Respuesta:** a) Una sola forma de venta por producto.

**Impacto:** no se agrega una tabla de presentaciones/unidades de venta.

### 8. Si al vender no alcanza el stock, ¿qué pasa?

- a) ★ Rechazar la venta con `409`, sin movimiento de stock. _Motivo: el stock nunca puede quedar negativo; es la regla general del proyecto y coincide con `incidencias`._
- b) (decisión original del relevamiento, **revertida el 26/11/2026**) Dejar confirmar con un aviso y stock negativo hasta que se ajuste.

**Respuesta vigente:** a) No se confirma la venta si no hay stock suficiente.

**Impacto:** cuando se implemente `VEN-10`, `StockService` valida el saldo antes de descontar y
devuelve `409` sin guardar nada. La respuesta b quedó como registro histórico: se resolvió la
contradicción con `PROMPT_CONTEXTO` ("stock nunca negativo") a favor de la regla general. En esta
tanda **no se descuenta stock** porque `VEN-10` todavía no está implementado, aunque el módulo
`stock` ya existe. CAJA-07 ya revierte las salidas con `id_venta` cuando las hay.

---

## D. Cómo se arma una venta

### 9. ¿Una venta puede quedar guardada sin confirmar?

- a) ★ No: se arma y se confirma en el momento. Estados: confirmada o anulada. _Motivo: para dejar algo pendiente ya existen Pedido y Presupuesto._

**Respuesta:** a) Alta y confirmación en el mismo paso. Estados `CONFIRMADA` / `ANULADA` (aclaración 6).

**Impacto:** enum `EstadoVenta { CONFIRMADA, ANULADA }`. No se persiste `PENDIENTE`.

### 10. ¿Quién figura como vendedor?

- a) ★ Siempre el usuario que carga la venta. _Motivo: es automático y coincide con la auditoría._

**Respuesta:** a) Usuario autenticado de la sesión.

**Impacto:** `id_vendedor` se toma de `UsuarioService.obtenerAutenticado()`, nunca del body del request.

---

## E. Pedidos y ventas

### 11. Cuando se entrega un pedido, ¿qué pasa con la venta?

- a) ★ Al entregarlo se genera la venta desde el pedido, con sus productos y precios; la venta guarda `id_pedido`. _Motivo: no se carga dos veces y queda la trazabilidad._

**Respuesta:** a) Venta generada desde el pedido con `id_pedido`. **Diferido:** ver `VEN-09`.

**Impacto:** la columna `id_pedido` debe estar prevista en el modelo de Vertabelo, aunque esta tanda no la use.

### 12. (clienta) ¿Los pedidos llevan seña?

- a) No, se cobra todo al entregar.
- b) Sí. La seña entra a la caja al aprobar el pedido y se descuenta al entregar.

**Respuesta:** b) Sí hay seña (aclaración 7). **Diferido:** ver `VEN-09`.

**Impacto:** requiere módulos `pedidos` y `caja`. No se implementa en esta tanda; queda registrado para no perder la decisión.

---

## F. Comprobantes

### 13. ¿Cuándo se genera el remito?

- a) ★ Automáticamente con cada venta. _Motivo: el PDF pide un remito asociado a cada venta o entrega._

**Respuesta:** a) Remito automático. **Diferido:** ver `VEN-06`.

### 14. (clienta) ¿Una venta se puede entregar en varias partes?

- b) Sí. Haría falta `Detalle_Remito` para saber qué se entregó en cada uno.

**Respuesta:** b) Varias entregas con `Detalle_Remito` (aclaración 8). **Diferido:** ver `VEN-07`.

### 15. (clienta) ¿Emiten factura?

- c) Sí, y el sistema tiene que emitirla.

**Respuesta:** c) Factura emitida por el sistema. **Diferido:** ver `VEN-08`.

---

## Aclaraciones sobre respuestas conflictivas

| # | Pregunta | Respuesta | Consecuencia |
|---|---|---|---|
| 1 | Precios: 2b vs 3 vs 6a | **Lista por tipo de cliente** | Prevalecen 2b y 3. Se descarta 6a: no hay campo `descuento` ni precio editable al vender. |
| 2 | Stock negativo vs regla general del contexto | **No se permite stock negativo** | Se revierte 8b el 26/11/2026. Prevalece la regla general; `VEN-10` actualizado en consecuencia. |
| 3 | Alcance de la primera tanda | **Solo venta + detalle** | Sin stock, pedidos, caja, remito ni factura. |
| 4 | Autorización para tocar otros módulos | **Solo leer otros módulos** | Se usan servicios públicos existentes. Si falta un método en `clientes`/`catalogo`, se avisa antes de agregarlo. |
| 5 | Fuente del esquema | **DDL de Vertabelo** | El usuario va a pasar el export; no se inventan tablas nuevas. |
| 6 | Estados | **CONFIRMADA y ANULADA** | Sin `PENDIENTE`, aunque `PROMPT_CONTEXTO` lo mencione. |
| 7 | Seña de pedidos | **Sí hay seña** | Se registra la decisión; implementación en `VEN-09`. |
| 8 | Comprobantes | **Remito + factura completos** | Incluye entregas parciales con `Detalle_Remito`; implementación en `VEN-06` a `VEN-08`. |
| 9 | Lista de precios vs DDL recibido | **Agregarla al modelo y re-exportar** | El DDL no traía la tabla. Se espera el export nuevo antes de `VEN-02`/`VEN-03`. |
| 10 | `fecha_vencimiento` de `Venta` | **Se mantiene: plazo del saldo con seña** | No hay cuenta corriente, pero cuando hay seña el cliente paga el resto en un plazo; esa fecha es el vencimiento. Sin regla de negocio en esta tanda (`VEN-09`). |
| 11 | PK de `Detalle_Venta` | **Agregar `id_detalle_venta`** | El DDL traía PK compuesta `(id_producto_fk, id_venta_fk)`. Se corregirá en el modelo para tener id propio. |
| 12 | Export desactualizado | **Actualizar el modelo ahora** | Se corrigen `Cliente.cuit`, `Insumo.stock_actual`, `Incidencia`, `Costo` y `Tipo_Incidencia` antes de seguir con ventas. |

> Las aclaraciones 7 y 8 contradicen la aclaración 3 en alcance, pero no en negocio: están aprobadas y se guardan como tickets bloqueados, no se descartan.

---

## Notas del diagrama

- `Cliente.cuit` era PK `int`: un CUIT tiene 11 dígitos y no entra en `int`, y un consumidor final no tiene CUIT. **Ya resuelto en `clientes`**: `id_cliente` serial como PK y `cuit` `varchar(13)` nulo y único.
- `Venta.medio_pago` se saca: ya decidido en `Caja`. **No se agrega** a `Venta`.
- `Venta` necesita `estado`: **confirmada / anulada**, según aclaración 6.
- `Venta.descuento` de la nota original: **se elimina**, según pregunta 3 y aclaración 1. El DDL recibido tampoco lo tiene.
- `Venta.medio_pago`: tampoco está en el DDL, coherente con que corresponde a `caja`.
- `Venta.fecha_vencimiento`: aparece en el DDL; según aclaración 10 es el plazo para pagar el saldo cuando hay seña. Se persiste como opcional.
- `Detalle_Venta.cantidad` era `int`: el DDL ya trae `decimal(12,3)` ✅.
- `Detalle_Venta.precio_historico`: foto del precio al vender, no editable.
- `Detalle_Venta` traía PK compuesta: se agrega `id_detalle_venta` (aclaración 11).
- `Venta` no traía `id_pedido`: se agregará cuando se modele `VEN-09`.
- `Producto.stock_actual` era `int`: **ya resuelto en `catalogo`**, es `BigDecimal(12,3)`; el DDL recibido todavía dice `int` y se corrige según aclaración 12.

# Backlog — Módulo Ventas

Tickets del módulo `venta` escritos como rebanadas verticales. Esta primera tanda cubre solo
**venta + detalle** (aclaración 3 de [`preguntas.md`](preguntas.md)): el alta se confirma en el
momento, sin descuento de stock, sin pedidos y sin comprobantes. La integración con caja
(CAJA-02) ya está incluida: exige sesión abierta y registra un ingreso por pago. Los temas
acordados pero diferidos quedan como tickets 🔴 con su bloqueante explícito.

**Decisiones vigentes de esta tanda:**

- Toda venta tiene un cliente: se usa el cliente `GENERICO` ("Consumidor final") para mostrador,
  o un cliente cargado. `id_cliente` es obligatorio y `Long`.
- El precio unitario sale de una **lista de precios por tipo de cliente**, no de `Producto.precio`
  directamente. Nadie lo edita al vender. **No hay descuento** y no se persiste campo `descuento`.
- `Detalle_Venta.cantidad` es `decimal(12,3)`: un producto se vende de una sola forma.
- Alta y confirmación ocurren en el mismo paso. Estados: `CONFIRMADA` / `ANULADA`, sin
  `PENDIENTE`.
- El vendedor se toma de `UsuarioService.obtenerAutenticado()`, nunca del body.
- Falta stock **no bloquea**: se confirma con aviso y el saldo quedaría negativo. En esta tanda
  **no se descuenta stock** (ver `VEN-10`).
- `Venta.medio_pago` no existe: corresponde a `caja`.
- Los totales se calculan en el backend desde los detalles; nunca se confían al cliente.

**Convención de nombres:** los identificadores (`id_venta`, `id_cliente`, `id_vendedor`,
`id_producto`, `id_pedido`, `id_remito`) van en `snake_case`; el resto de campos, métodos y claves
JSON van en `camelCase`. Las columnas de la base conservan `snake_case` mediante
`@Column(name = ...)`.

> El documento [`preguntas.md`](preguntas.md) quedó como registro del relevamiento; este backlog
> refleja lo que se va a implementar.

Leyenda de estado: ✅ Hecho · 🟡 En curso · 🟢 Listo · 🔴 Bloqueado.

## Resumen

| ID | Ticket | Bloqueado por | Estado |
|---|---|---|---|
| [`VEN-01`](VEN-01-modelo-venta-detalle.md) | Modelo de `Venta` y `Detalle_Venta` | — | ✅ |
| [`VEN-02`](VEN-02-lista-precios-tipo-cliente.md) | Lista de precios por tipo de cliente | Modelo de datos | 🟡 |
| [`VEN-03`](VEN-03-alta-y-confirmacion.md) | Alta y confirmación de venta | `VEN-01`, `VEN-02` | ✅ |
| [`VEN-04`](VEN-04-anulacion.md) | Anulación de venta | `VEN-03` | ✅ |
| [`VEN-05`](VEN-05-consultas-y-filtros.md) | Consultas y filtros de ventas | `VEN-03` | ✅ |
| [`VEN-06`](VEN-06-remito-automatico.md) | Remito automático por venta | Módulo de comprobantes | 🔴 |
| [`VEN-07`](VEN-07-entregas-parciales.md) | Entregas parciales con `Detalle_Remito` | `VEN-06` | 🔴 |
| [`VEN-08`](VEN-08-factura.md) | Emisión de factura | `VEN-06`, módulo de facturación | 🔴 |
| [`VEN-09`](VEN-09-venta-desde-pedido.md) | Venta desde pedido con seña | módulos `pedidos` y `caja` | 🔴 |
| [`VEN-10`](VEN-10-descuento-stock.md) | Descuento de stock al confirmar | módulo `stock` (`STOCK-01`) | 🔴 |
| [`VEN-11`](VEN-11-tests.md) | Tests del módulo | `VEN-03`, `VEN-04`, `VEN-05` | ✅ |
| [`VEN-12`](VEN-12-documentacion-swagger.md) | Documentación Swagger | `VEN-03`, `VEN-04`, `VEN-05` | ✅ |
| [`VEN-13`](VEN-13-frontend-ventas.md) | Frontend de ventas | `VEN-03`, `VEN-05` (resueltos) | ✅ |

## Orden recomendado

1. ~~`VEN-01`~~ (hecho con el DDL recibido).
2. `VEN-02` (resolución de precios hecha; falta la tabla en el modelo y el CRUD de carga).
3. ~~`VEN-03`~~.
4. ~~En paralelo: `VEN-04` y `VEN-05`~~.
5. ~~En paralelo: `VEN-11` y `VEN-12`~~.
6. ~~`VEN-13`~~ (hecho).
7. Los diferidos (`VEN-06` a `VEN-10`) cuando existan sus módulos y su DDL.

**Camino crítico cumplido hasta acá:** `VEN-01 → VEN-03 → VEN-04/VEN-05 → VEN-11/VEN-12 → VEN-13`.
Lo que queda: `VEN-02` y los diferidos (`VEN-06` a `VEN-10`).

## Prerrequisitos (fuera del código de `venta`)

### 1. Estado del modelo de datos

Se recibieron dos exports de Redgate Data Modeler (2026-11-19). El código de `venta` usa el
segundo. **El modelo sigue sin finalizar**, por lo que el código avanzó con lo disponible y acá
quedan los ajustes pendientes:

| Ajuste | Estado en el último DDL |
|---|---|
| `Cliente.cuit` nullable | ✅ Corregido |
| `Detalle_Venta.id_detalle_venta` como PK | ✅ Corregido |
| `Detalle_Remito.id_detalle_remito serial` y columna `id_venta` | ✅ Corregido |
| FK `Remito_Comprobante_Cliente` | ✅ Corregida |
| **Lista de precios por tipo de cliente** | ❌ Falta: `VEN-02` la creó por JPA, pero no está en el modelo |
| `Insumo.stock_actual` | ❌ Quedó `decimal(%,%)`: valor inválido, debe ser `decimal(12,3)` |
| `Producto.stock_actual` | ❌ Volvió a `int`; el código usa `BigDecimal(12,3)` y se vende por peso |
| `Detalle_Remito.cantidad` | ❌ Sigue `int`; Vertabelo tiró error al cambiarlo a decimal |
| `Incidencia` y `Tipo_Incidencia` | ❌ Estructura vieja (`cantidad_afectada int`, FK invertida) |
| `Costo.id_usuario_anulacion` | ❌ Sigue `NOT NULL`; el código lo deja nullable |
| `id_pedido` en `Venta` y seña en `Pedido` | ❌ Para `VEN-09` |

`Venta.fecha_vencimiento` se mantiene: es el plazo para pagar el saldo cuando hay seña
(aclaración 10). Se persiste como opcional, sin regla de negocio en esta tanda.

### 2. Cliente genérico accesible — resuelto en `CLI-09`

El usuario autorizó tocar `clientes`. Ya están hechos:

- `ClienteService.obtenerGenerico()` devuelve la entidad reservada y falla con 404 si no existe.
- `ClienteGenericoInitializer` la crea al arrancar si falta, con `cuit` nulo y tipo `GENERICO`.

`VEN-03` puede usar el servicio sin depender de un id cargado a mano.

### 3. Dueño de la lista de precios — pendiente de decidir

La tabla `Precio_Tipo_Cliente` la crea JPA con `ddl-auto=update` y hoy vive en el paquete
`venta`. Falta decidir si su CRUD queda ahí o pasa a `catalogo`/`clientes`, y reflejar la tabla en
el export. Si pasa a otro módulo, se necesita permiso para agregar el servicio de consulta (la
autorización de esta tanda era **solo leer**).

## Verificación del módulo

Estado actual: `./mvnw clean compile` pasa y `./mvnw -Dtest='com.taller.gestion.venta.**' test`
da **15 tests, 0 fallos** (10 de service y 5 de controller).

```bash
cd backend
./mvnw clean compile
./mvnw -Dtest='com.taller.gestion.venta.**' test
```

También pasan los 48 tests del módulo `clientes` después de `CLI-09`.

Para el frontend (`VEN-13`):

```bash
cd frontend
npm run lint
npm run build
```

Ambos pasan. La pantalla quedó en `/ventas` con link en la navbar.

Prueba manual en una base limpia:

1. Arrancar backend e iniciar sesión como vendedor.
2. Dar de alta una venta de mostrador sin cargar cliente: debe usar el genérico "Consumidor final".
3. Dar de alta una venta a un gran cliente: el precio unitario debe salir de su lista por tipo.
4. Intentar cambiar el precio en el body: debe ignorarse; no existe campo de descuento.
5. Cargar una cantidad decimal: el total debe calcularse en backend con `BigDecimal`.
6. Vender más de lo que hay en stock: debe confirmar con aviso, sin 409.
7. Anular la venta: queda `ANULADA`, no se borra y no se puede volver a anular.
8. Verificar que `id_vendedor` es el usuario de la sesión, no el del body.

## Fuera de alcance de esta tanda

- Descuento de stock y movimientos de stock (`VEN-10`, depende de `STOCK-01`).
- Remito automático, entregas parciales y factura (`VEN-06`, `VEN-07`, `VEN-08`).
- Pedidos, seña y `id_pedido` (`VEN-09`).
- Medio de pago y cobro (corresponde a `caja`).
- Frontend de ventas (`VEN-13`).
- No se modifica `PROMPT_CONTEXTO.md`; sus reglas generales sobre stock quedan desactualizadas
  hasta que se revise por separado.

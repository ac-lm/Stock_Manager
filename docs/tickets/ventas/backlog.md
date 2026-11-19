# Backlog — Módulo Ventas

Tickets del módulo `venta` escritos como rebanadas verticales. Esta primera tanda cubre solo
**venta + detalle** (aclaración 3 de [`preguntas.md`](preguntas.md)): el alta se confirma en el
momento, sin descuento de stock, sin pedidos, sin caja y sin comprobantes. Los temas acordados
pero diferidos quedan como tickets 🔴 con su bloqueante explícito.

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
| `VEN-01` | Modelo de `Venta` y `Detalle_Venta` | DDL de Vertabelo | 🔴 |
| `VEN-02` | Lista de precios por tipo de cliente | DDL de Vertabelo + definir módulo dueño | 🔴 |
| `VEN-03` | Alta y confirmación de venta | `VEN-01`, `VEN-02`, cliente genérico accesible | 🔴 |
| `VEN-04` | Anulación de venta | `VEN-03` | 🔴 |
| `VEN-05` | Consultas y filtros de ventas | `VEN-03` | 🔴 |
| `VEN-06` | Remito automático por venta | `VEN-03`, DDL de remito | 🔴 |
| `VEN-07` | Entregas parciales con `Detalle_Remito` | `VEN-06`, DDL de remito | 🔴 |
| `VEN-08` | Emisión de factura | `VEN-06`, módulo de facturación | 🔴 |
| `VEN-09` | Venta desde pedido con seña | módulos `pedidos` y `caja` | 🔴 |
| `VEN-10` | Descuento de stock al confirmar | módulo `stock` (`STOCK-01`) | 🔴 |
| `VEN-11` | Tests del módulo | `VEN-03`, `VEN-04`, `VEN-05` | 🔴 |
| `VEN-12` | Documentación Swagger | `VEN-03`, `VEN-04`, `VEN-05` | 🔴 |
| `VEN-13` | Frontend de ventas | `VEN-03`, `VEN-05` | 🔴 |

## Orden recomendado

1. **DDL de Vertabelo** (prerrequisito, no es un ticket de código).
2. Resolver el cliente genérico accesible y el dueño de la lista de precios.
3. `VEN-01` y `VEN-02` (en paralelo).
4. `VEN-03`.
5. **En paralelo:** `VEN-04` y `VEN-05`.
6. **En paralelo:** `VEN-11` y `VEN-12`.
7. `VEN-13` cuando se habilite el frontend de ventas.
8. Los diferidos (`VEN-06` a `VEN-10`) cuando existan sus módulos y su DDL.

**Camino crítico:** `DDL → VEN-01/VEN-02 → VEN-03 → VEN-05 → VEN-11/VEN-12`.

## Prerrequisitos (fuera del código de `venta`)

### 1. DDL de Vertabelo — bloquea `VEN-01` y `VEN-02`

El usuario va a pasar el export/DDL. No se inventan estructuras. Debe cubrir:

- `Venta` y `Detalle_Venta` (incluido `decimal(12,3)` en cantidad).
- La **lista de precios por tipo de cliente**: tabla, columnas y relación con `TipoCliente`.
- `Remito` y `Detalle_Remito` (aunque sean diferidos, para no rediseñar después).
- `id_pedido` en `Venta` (diferido, pero debe quedar previsto).
- Confirmar si `Venta.id_estado`/`estado` es `varchar` o enum, y si `id_vendedor` e
  `id_cliente` son FK o solo `Long` como en `incidencias`.

Hasta que llegue, **no se escriben entidades nuevas de ventas**. El esqueleto actual (`Double`,
`estado` como `String`, sin detalle) se reemplaza, no se parchea.

### 2. Cliente genérico accesible — bloquea `VEN-03`

`clientes` define `TipoCliente.GENERICO` y prohíbe usarlo desde la API, pero:

- `ClienteService` no expone un `obtenerGenerico()` ni un `findByTipoCliente`.
- No hay inicializador (`CommandLineRunner`, `@PostConstruct`) que cree el cliente `GENERICO` en
  la base.

El usuario autorizó **solo leer** otros módulos en esta tanda. Para el alta de venta hay que
elegir una de estas opciones antes de `VEN-03`:

1. Agregar un método de solo lectura en `ClienteService` + un inicializador en `clientes`
   (**requiere autorización para tocar `clientes`**).
2. Resolverlo dentro de `venta` sin modificar `clientes` (por ejemplo, recibir `id_cliente` del
   body para mostrador, con el id del genérico cargado por configuración). Menos automático.

### 3. Dueño de la lista de precios — bloquea `VEN-02`

La tabla de lista de precios por tipo de cliente no existe. Hasta ver el DDL no se sabe si vive en
`catalogo`, en `clientes` o en `venta`. Con la autorización actual (**solo leer**), `venta` puede
consultar servicios existentes pero no agregar métodos en esos módulos.

## Verificación del módulo

Cuando los tickets estén hechos:

```bash
cd backend
./mvnw clean compile
./mvnw -Dtest='com.taller.gestion.venta.**' test
```

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

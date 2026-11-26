# Modelo de datos

El diseño completo se hace en **Vertabelo** y es la fuente de verdad
(ver [`db/README.md`](db/README.md)). Este documento describe las tablas que ya están
implementadas en el backend y las decisiones tomadas al mapearlas.

## Convenciones

- Nombres de tablas y columnas en `PascalCase`/`snake_case`, tal como vienen del modelo.
- IDs `Long`, autogenerados con `GenerationType.IDENTITY`.
- En Java los atributos van en `camelCase` (`stockActual`), pero la columna conserva el nombre
  del modelo (`stock_actual`) vía `@Column(name = ...)`.
- Los IDs mantienen el guion bajo también en Java, DTOs, JSON y JPQL: `id_insumo`,
  `id_producto_insumo`, etc.
- Las relaciones entre módulos se resuelven con `@ManyToOne` a la entidad real, no con un `Long`
  suelto (excepto en `Venta`, que todavía está en su versión inicial).

## Tablas implementadas

### `Insumo` (módulo `catalogo`)

Materias primas que consume la producción.

| Columna | Tipo | Restricciones |
|---|---|---|
| `id_insumo` | `bigint` | PK, autoincremental |
| `nombre` | `varchar(50)` | not null |
| `stock_actual` | `integer` | not null |
| `unidad_medida` | `varchar(50)` | not null |
| `stock_minimo` | `integer` | not null |
| `costo` | `numeric(12,2)` | not null |

Al eliminar un insumo se eliminan en cascada sus filas de `Producto_Insumo` y
`Proveedor_Insumo` (relaciones `@OneToMany` con `orphanRemoval`).

### `Producto` (módulo `catalogo`)

Productos terminados.

| Columna | Tipo | Restricciones |
|---|---|---|
| `id_producto` | `bigint` | PK, autoincremental |
| `nombre` | `varchar(50)` | not null |
| `precio` | `numeric(12,2)` | not null |
| `stock_actual` | `numeric(12,3)` | not null |
| `categoria` | `varchar(50)` | not null |

Al eliminar un producto se eliminan en cascada sus filas de `Producto_Insumo`.

### `Producto_Insumo` (módulo `catalogo`)

Receta: qué insumos y en qué cantidad necesita cada producto.

| Columna | Tipo | Restricciones |
|---|---|---|
| `id_producto_insumo` | `bigint` | PK, autoincremental |
| `id_producto` | `bigint` | FK → `Producto`, not null |
| `id_insumo` | `bigint` | FK → `Insumo`, not null |
| `cant_necesaria` | `real` | not null |
| `unidad_medida` | `varchar(50)` | not null |

### `Proveedor` (módulo `proveedores`)

| Columna | Tipo | Restricciones |
|---|---|---|
| `id_proveedor` | `bigint` | PK, autoincremental |
| `nombre` | `varchar(50)` | not null |
| `CUIT` | `varchar(13)` | not null, **unique** |
| `telefono` | `varchar(50)` | not null |
| `email` | `varchar(50)` | nullable |
| `tipo_producto` | `varchar(50)` | not null |
| `observaciones` | `varchar(50)` | nullable |

El modelo no define columna de estado: la baja es **física** y arrastra en cascada sus
filas de `Proveedor_Insumo`.

### `Proveedor_Insumo` (módulo `proveedores`)

Qué insumos provee cada proveedor.

| Columna | Tipo | Restricciones |
|---|---|---|
| `id_proveedor_insumo` | `bigint` | PK, autoincremental |
| `id_proveedor` | `bigint` | FK → `Proveedor`, not null |
| `id_insumo` | `bigint` | FK → `Insumo`, not null |

### `Incidencia` (módulo `incidencias`)

| Columna | Tipo | Restricciones |
|---|---|---|
| `id_incidencia` | `bigint` | PK, autoincremental |
| `id_insumo` | `bigint` | FK → `Insumo`, nullable |
| `tipo_incidencia` | `varchar(50)` | not null |
| `descripcion` | `varchar(100)` | nullable |
| `cantidad_afectada` | `integer` | nullable |
| `estado` | `varchar(50)` | nullable |
| `observaciones` | `varchar(100)` | nullable |
| `resolucion_adoptada` | `varchar(100)` | nullable |

> La relación con `Producto` está comentada en la entidad porque el modelo original la contempla,
> pero todavía no se habilitó en el código.

### `Usuario` (módulo `usuario`)

| Columna | Tipo | Restricciones |
|---|---|---|
| `id_usuario` | `bigint` | PK, autoincremental |
| `nombre` | `varchar(100)` | not null |
| `email` | `varchar(255)` | not null, **unique**; se usa como nombre de usuario |
| `password` | `varchar(255)` | not null; se guarda con BCrypt |
| `rol` | `varchar(20)` | not null; `ADMINISTRADOR`, `VENDEDOR` |
| `activo` | `boolean` | not null, default `true` |

La baja es lógica para no perder el historial de ventas, movimientos y auditoría. El login usa
sesión HTTP con cookie `HttpOnly`; la sesión se valida contra la base en cada request.

### `Cliente` (módulo `clientes`)

| Columna | Tipo | Restricciones |
|---|---|---|
| `id_cliente` | `bigint` | PK, autoincremental |
| `CUIT` | `varchar(13)` | nullable, **unique**; nulo solo para consumidor final |
| `nombre` | `varchar(50)` | not null |
| `telefono` | `varchar(30)` | nullable |
| `email` | `varchar(150)` | not null |
| `direccion` | `varchar(80)` | not null |
| `tipo_cliente` | `varchar(20)` | not null; enum `TipoCliente` |
| `observaciones` | `varchar(250)` | nullable |
| `activo` | `boolean` | not null, default `true` |
| `plazo_pago_dias` | `integer` | nullable |
| `nombre_normalizado` | `varchar(50)` | not null; se recalcula en cada alta/edición |

La baja es lógica (`activo`). El cliente genérico de consumidor final se crea al arrancar y no
se puede modificar ni desactivar.

### `Venta` (módulo `venta`)

| Columna | Tipo | Restricciones |
|---|---|---|
| `id_venta` | `bigint` | PK, autoincremental |
| `id_cliente` | `bigint` | not null; FK validada por `ClienteService` |
| `id_vendedor` | `bigint` | not null; usuario autenticado |
| `fecha` | `date` | not null |
| `hora` | `time` | not null |
| `total` | `numeric(12,2)` | not null |
| `estado` | `varchar(20)` | not null; `CONFIRMADA`, `ANULADA` |
| `fecha_vencimiento` | `date` | nullable; se usa cuando la venta viene de un pedido |

Los renglones viven en `Detalle_Venta` con `@OneToMany` y `orphanRemoval`. Al confirmar la venta
se exige una sesión `Sesion_Caja` `ABIERTA` y se crea un `Movimiento_Caja` `INGRESO`/`VENTA` por
cada pago, con `id_venta`; venta y movimientos se guardan en la misma transacción. No existe
`Venta.medio_pago` ni una tabla de pagos: los pagos se consultan desde `Movimiento_Caja` por
`id_venta` y se exponen en `VentaResponseDTO.pagos`. La anulación con contramovimientos queda
para CAJA-07.

### `Costo` (módulo `costos`)

| Columna | Tipo | Restricciones |
|---|---|---|
| `id_costo` | `bigint` | PK, autoincremental |
| `id_tipo_costo` | `bigint` | not null, FK → `TipoCosto` |
| `descripcion` | `varchar(255)` | not null |
| `monto` | `numeric(12,2)` | not null |
| `fecha_pago` | `date` | not null |
| `periodo_mes` | `integer` | not null |
| `periodo_anio` | `integer` | not null |
| `id_usuario` | `bigint` | not null, FK → `Usuario` |
| `id_ingreso_mercaderia` | `bigint` | nullable, **unique**; vínculo con el ingreso que lo generó |
| `anulado` | `boolean` | not null, default `false` |
| `motivo_anulacion` | `varchar(255)` | nullable |
| `id_usuario_anulacion` | `bigint` | nullable, FK → `Usuario` |
| `fecha_anulacion` | `timestamp` | nullable |
| `observaciones` | `varchar(255)` | nullable |

La baja es lógica (`anulado`). El vínculo entre un egreso de caja y un costo
(`Movimiento_Caja.id_costo`) queda para CST-05.

### `Sesion_Caja` (módulo `caja`)

Una sesión de caja es el período entre una apertura y su cierre. Nunca hay dos sesiones
`ABIERTA` al mismo tiempo: la unicidad se protege con `pg_advisory_xact_lock(7401)` dentro de
la transacción de apertura, no con una constraint de base.

| Columna | Tipo | Restricciones |
|---|---|---|
| `id_sesion` | `bigint` | PK, autoincremental |
| `id_usuario_apertura` | `bigint` | not null, FK → `Usuario` |
| `id_usuario_cierre` | `bigint` | nullable, FK → `Usuario` |
| `id_usuario_revision` | `bigint` | nullable, FK → `Usuario` |
| `fecha_apertura` | `date` | not null |
| `hora_apertura` | `time` | not null |
| `fecha_cierre` | `date` | nullable |
| `hora_cierre` | `time` | nullable |
| `monto_inicial` | `numeric(12,2)` | not null |
| `motivo_ajuste_inicial` | `varchar(150)` | nullable; obligatorio si el monto difiere del sugerido |
| `estado` | `varchar(30)` | `ABIERTA`, `PENDIENTE_REVISION`, `CERRADA` |
| `total_esperado` | `numeric(12,2)` | nullable hasta cerrar |
| `total_declarado` | `numeric(12,2)` | nullable hasta cerrar |
| `diferencia` | `numeric(12,2)` | nullable hasta cerrar; declarado − esperado |
| `observaciones` | `varchar(100)` | nullable; obligatoria si hay diferencia |
| `fecha_revision` | `timestamp` | nullable; guarda fecha y hora |
| `observacion_revision` | `varchar(150)` | nullable |

`PENDIENTE_REVISION` ya no admite movimientos y no bloquea la apertura de la sesión siguiente.
La revisión solo la puede hacer un administrador y no cambia los montos.

### `Movimiento_Caja` (módulo `caja`)

| Columna | Tipo | Restricciones |
|---|---|---|
| `id_movimiento_caja` | `bigint` | PK, autoincremental |
| `id_sesion_caja` | `bigint` | not null, FK → `Sesion_Caja` |
| `id_usuario` | `bigint` | not null, FK → `Usuario` |
| `id_venta` | `bigint` | nullable; se usa en Fase 2 |
| `id_costo` | `bigint` | nullable; se usa con CST-05 |
| `tipo` | `varchar(20)` | `INGRESO`, `EGRESO` |
| `concepto` | `varchar(20)` | `VENTA`, `RETIRO`, `GASTO`, `ANULACION` |
| `monto` | `numeric(12,2)` | not null, mayor a 0 |
| `medio_pago` | `varchar(20)` | `EFECTIVO`, `TRANSFERENCIA` |
| `fecha` | `date` | not null |
| `hora` | `time` | not null |
| `observaciones` | `varchar(150)` | nullable |

Los movimientos no se editan ni se borran: una anulación se registra con un contramovimiento.
En Fase 1 solo se crean egresos `RETIRO`/`GASTO`; los ingresos por venta llegan con CAJA-02.

### `Cierre_Caja_Detalle` (módulo `caja`)

| Columna | Tipo | Restricciones |
|---|---|---|
| `id_sesion` | `bigint` | PK, FK → `Sesion_Caja` |
| `medio_pago` | `varchar(20)` | PK; `EFECTIVO` o `TRANSFERENCIA` |
| `total_esperado` | `numeric(12,2)` | not null |
| `total_declarado` | `numeric(12,2)` | not null |
| `diferencia` | `numeric(12,2)` | not null; declarado − esperado |

La clave es compuesta (`id_sesion`, `medio_pago`). Siempre se crean las dos filas al cerrar,
aunque algún medio quede en cero. El `total_declarado` de `TRANSFERENCIA` es el neto de la
sesión (ingresos − egresos) y puede ser negativo; el de `EFECTIVO` no puede ser negativo.

## Tablas pendientes

Además de las documentadas, el modelo de Vertabelo contempla tablas que todavía no están
implementadas en el backend:

- `Movimiento_Stock`: estructura propuesta y decisiones en
  [`tickets/stock/backlog.md`](tickets/stock/backlog.md#stock-01--modelo-y-entidad-movimiento_stock).
  Se documenta acá una vez implementada.
- `Pedido` y `Detalle_Pedido`.
- `Auditoria`.
- `Ingreso_Mercaderia` y las tablas restantes del modelo.

No se documentan columnas de esas tablas hasta implementarlas, para no inventar estructura.

## Reglas de borrado

| Tabla | Tipo de baja | Motivo |
|---|---|---|
| `Insumo`, `Producto`, `Producto_Insumo` | física | el modelo no define estado |
| `Proveedor`, `Proveedor_Insumo` | física | el modelo no define estado |
| `Cliente` | lógica (`activo`) | el modelo sí define estado |
| `Costo` | lógica (`anulado`) | conserva el historial de gastos |
| `Movimiento_Caja` | no se borra | trazabilidad de la caja |
| `Sesion_Caja`, `Cierre_Caja_Detalle` | no se borra | historial de cierres |

Las tablas con baja física eliminan sus dependencias en cascada.

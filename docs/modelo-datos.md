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
| `nombre` | `varchar(255)` | nullable (sin `@Column` explícito) |
| `email` | `varchar(255)` | nullable |
| `password` | `varchar(255)` | nullable |
| `rol` | según enum `Rol` | `ADMINISTRADOR`, `VENDEDOR` |

Estructura inicial: todavía no hay login, seguridad ni validaciones. El enum `Rol` no tiene
`@Enumerated`, por lo que Hibernate lo persiste como ordinal; revisar al implementar el módulo.

### `Venta` (módulo `venta`)

| Columna | Tipo | Restricciones |
|---|---|---|
| `id_venta` | `bigint` | PK, autoincremental |
| `id_cliente` | `bigint` | sin FK en el código actual |
| `fecha` | `date` | — |
| `hora` | `time` | — |
| `total` | `double precision` | — |
| `id_vendedor` | `bigint` | sin FK en el código actual |
| `estado` | `varchar(255)` | — |
| `descuento` | `double precision` | — |

Es una primera versión; falta modelar las relaciones con `Cliente` y `Usuario`.

## Tablas pendientes

El modelo de Vertabelo tiene **22 tablas**. Además de las de arriba, el sistema contempla:

- `Cliente` (con columna `activo` → baja lógica).
- `Movimiento_Stock`, `Caja`, `Pedido`, `Detalle_Pedido`, `Costo`, `Auditoria` y las tablas
  restantes del modelo.

No se documentan columnas de esas tablas hasta contar con el XML completo, para no inventar
estructura. Cuando se agregue `docs/db/modelo-vertabelo.xml`, esta sección se completa con el
detalle exacto.

## Reglas de borrado

| Tabla | Tipo de baja | Motivo |
|---|---|---|
| `Insumo`, `Producto`, `Producto_Insumo` | física | el modelo no define estado |
| `Proveedor`, `Proveedor_Insumo` | física | el modelo no define estado |
| `Cliente` | lógica (`activo`) | el modelo sí define estado |

Las tablas con baja física eliminan sus dependencias en cascada.

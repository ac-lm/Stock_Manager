# Backlog — Módulo Stock

Tickets del módulo `stock` escritos como rebanadas verticales. Esta primera tanda es el
**alcance elegido (opción B del relevamiento)**: núcleo de movimientos con ajuste manual,
consultas/kardex y pantalla. La única integración incorporada después es CAJA-07 (devolución al
anular ventas); siguen pendientes `VEN-10`, `INC-07` y `CST-06`.

**Decisiones vigentes:**

- Un único servicio (`StockService`) modifica `stock_actual`. Los movimientos y el cambio de
  saldo ocurren en la **misma transacción**.
- **No se permite stock negativo.** Una salida que supere el saldo devuelve `409` y no guarda
  ni el movimiento ni el cambio.
- Los movimientos son **inmutables**: no se editan ni se borran. Las correcciones se hacen con
  otro movimiento.
- Insumos y productos se consultan **por separado**, con pantallas distintas:
  `/stock/insumos` y `/stock/productos`, visibles para `ADMINISTRADOR` y `VENDEDOR`.
- El usuario de cada movimiento se toma de la **sesión actual**, nunca del cuerpo del request.
- Los endpoints de `/api/stock/**` requieren autenticación (sumar la regla en `SecurityConfig`).
- La estructura de `Movimiento_Stock` propuesta en `STOCK-01` queda **aceptada para esta tanda**;
  si aparece el export de Vertabelo se ajusta antes de generar la entidad.

**Convención de nombres:** identificadores (`id_movimiento_stock`, `id_producto`, `id_insumo`,
`id_usuario`, `id_venta`, `id_ingreso_mercaderia`) en `snake_case`; el resto de campos, métodos y
claves JSON en `camelCase`, con `@Column(name = ...)` para las columnas. DTOs como `record`.

> **Nota sobre el modelo de datos:** `docs/modelo-datos.md` documenta las tablas *implementadas*;
> `Movimiento_Stock` figura en **Tablas pendientes** sin definición de columnas. `docs/db/README.md`
> indica que la fuente de verdad es el XML exportado por Vertabelo y que todavía no está
> versionado en `docs/db/`. `STOCK-01` propone una estructura basada en el ticket del backlog de
> incidencias y en las convenciones del proyecto; quedó aceptada en la revisión del backlog. El
> único punto abierto del modelo es contrastarla con el export cuando se disponga de él.

Leyenda de estado: ✅ Hecho · 🟡 En curso · 🟢 Listo · 🔴 Bloqueado.

## Resumen

| ID | Ticket | Bloqueado por | Estado |
|---|---|---|---|
| [STOCK-01](#stock-01--modelo-y-entidad-movimiento_stock) | Modelo y entidad `Movimiento_Stock` | Propuesta aceptada; ajustar si aparece el XML | ✅ |
| [STOCK-02](#stock-02--servicio-unico-de-movimientos) | Servicio único de movimientos | 01 | ✅ |
| [STOCK-03](#stock-03--api-rest-y-kardex) | API REST y kardex | 01, 02 | ✅ |
| [STOCK-04](#stock-04--catalogo-sin-edicion-directa-de-stock) | Catálogo sin edición directa de stock | 02 | ✅ |
| [STOCK-05](#stock-05--pantallas-de-stock) | Pantallas de stock de insumos y productos | 03 | ✅ |
| [STOCK-06](#stock-06--tests) | Tests del módulo | 02, 03, 04 | ✅ |
| [STOCK-07](#stock-07--documentacion-y-swagger) | Documentación y Swagger | 01, 03, 05 | ✅ |

## Orden recomendado

1. `STOCK-01` (confirmar la estructura de columnas antes de generar la entidad).
2. `STOCK-02`.
3. En paralelo: `STOCK-03` y `STOCK-04`.
4. `STOCK-05`.
5. En paralelo: `STOCK-06` y `STOCK-07`.

**Camino crítico:** `STOCK-01 → STOCK-02 → STOCK-03 → STOCK-05`.

## Prerrequisitos de otros módulos

- `catalogo`: `Producto` y `Insumo` con `stock_actual numeric(12,3)` y sus servicios ya existen.
- `usuario`: `UsuarioService.obtenerAutenticado()` ya existe; con eso se resolvió el bloqueo
  histórico de `STOCK-01` por `USU-01`.
- PostgreSQL con las FK a `Usuario`, `Producto` e `Insumo` mapeadas con `@ManyToOne` (sesión
  HTTP ya configurada).
- Sin migraciones Flyway: el esquema se mantiene con `ddl-auto=update`, igual que en caja.

## Tickets

### STOCK-01 — Modelo y entidad `Movimiento_Stock`

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho (propuesta aceptada; ajustar si aparece el export de Vertabelo) |
| Bloqueado por | Validación del modelo de datos |
| Bloquea | `STOCK-02`, `STOCK-03` |
| Etiquetas | `stock`, `backend`, `db` |

**Objetivo:** definir la tabla de trazabilidad y mapearla como entidad JPA.

**Estructura propuesta (aceptada; ajustar si aparece el export de Vertabelo):**

| Columna | Tipo | Restricciones |
|---|---|---|
| `id_movimiento_stock` | `bigint` | PK, autoincremental |
| `id_producto` | `bigint` | FK → `Producto`, nullable |
| `id_insumo` | `bigint` | FK → `Insumo`, nullable |
| `tipo` | `varchar(10)` | `ENTRADA`, `SALIDA`; not null |
| `cantidad` | `numeric(12,3)` | not null, mayor a cero |
| `motivo` | `varchar(30)` | `INICIAL`, `COMPRA`, `AJUSTE`, `MERMA`, `DEVOLUCION`; not null |
| `id_usuario` | `bigint` | FK → `Usuario`, not null |
| `fecha_hora` | `timestamp` | not null |
| `observaciones` | `varchar(150)` | nullable |
| `id_venta` | `bigint` | FK → `Venta`, nullable; reservado para `VEN-10` |
| `id_ingreso_mercaderia` | `bigint` | nullable; reservado para `CST-06` |

**Reglas:**

- `CHECK`: exactamente uno de `id_producto` o `id_insumo` debe estar presente.
- Enums `TipoMovimientoStock` y `MotivoMovimientoStock`, persistidos como `STRING`.
- Relaciones `@ManyToOne` a `Producto`, `Insumo` y `Usuario`, según la convención del proyecto.
- Un único `fecha_hora timestamp not null`, según la decisión registrada en `preguntas.md`.

**Criterios de aceptación:**

- [ ] La estructura quedó validada contra el XML/PDF del modelo (o contra la decisión explícita
      de usar esta propuesta).
- [ ] La entidad compila con `./mvnw compile`.
- [ ] Nombres de tabla y columnas alineados al modelo.
- [ ] `docs/modelo-datos.md` pasa a documentar `Movimiento_Stock` (se hace en `STOCK-07`).

---

### STOCK-02 — Servicio único de movimientos

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | `STOCK-01` |
| Bloquea | `STOCK-03`, `STOCK-04`, `STOCK-06` |
| Etiquetas | `stock`, `backend` |

**Objetivo:** que ningún módulo toque `stock_actual` sin pasar por `StockService`.

**Alcance:**

- `StockService.registrarMovimiento(...)` para producto o insumo: valida cantidad positiva,
  exactamente un ítem, tipo y motivo válidos, y usuario de la sesión.
- Bloqueo pesimista (`PESSIMISTIC_WRITE`) sobre la fila de `Producto`/`Insumo` para evitar
  condiciones de carrera.
- `ENTRADA` suma; `SALIDA` valida saldo suficiente y resta. Saldo insuficiente →
  `OperacionNoPermitidaException` (`409`) con rollback.
- Guardar entidad y movimiento en la misma transacción `@Transactional`.
- Métodos públicos de uso interno para futuras integraciones (`descontarProducto`,
  `ingresarInsumo`, etc.), que hoy nadie más llama.

**Criterios de aceptación:**

- [ ] Una salida con saldo insuficiente no persiste nada y devuelve `409`.
- [ ] El movimiento queda con usuario y `fecha_hora` de la operación.
- [ ] Ningún otro servicio modifica `stock_actual` (ver `STOCK-04`).
- [ ] Los tests cubren entrada, salida, saldo insuficiente y rollback.

---

### STOCK-03 — API REST y kardex

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | `STOCK-01`, `STOCK-02` |
| Bloquea | `STOCK-05`, `STOCK-06`, `STOCK-07` |
| Etiquetas | `stock`, `backend` |

**Objetivo:** consultar saldos y movimientos, y registrar ajustes manuales.

**Endpoints (todos autenticados):**

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/stock/insumos` | Saldos de insumos con mínimo y flag `stockBajoMinimo` |
| GET | `/api/stock/productos` | Saldos de productos |
| POST | `/api/stock/insumos/movimientos` | Registrar entrada/salida de un insumo |
| POST | `/api/stock/productos/movimientos` | Registrar entrada/salida de un producto |
| GET | `/api/stock/insumos/{id_insumo}/movimientos` | Kardex de un insumo |
| GET | `/api/stock/productos/{id_producto}/movimientos` | Kardex de un producto |
| GET | `/api/stock/movimientos` | Historial con filtros y paginación |

**Filtros del historial:** `tipo`, `motivo`, `id_producto`, `id_insumo`, `id_usuario`, `desde`,
`hasta` y `Pageable`.

**DTOs:** `MovimientoStockCreateDTO`, `MovimientoStockResponseDTO`, `StockInsumoResponseDTO`,
`StockProductoResponseDTO`. El `id_usuario` del create se ignora: se toma de la sesión.

**Criterios de aceptación:**

- [ ] `201` al registrar, `200` en consultas, `400` por datos inválidos, `404` por id inexistente,
      `409` por saldo insuficiente.
- [ ] Sin sesión, los endpoints devuelven `401`.
- [ ] El historial se puede filtrar y ordenar por `fecha_hora` descendente.
- [ ] Swagger documenta todos los endpoints (se cierra en `STOCK-07`).

---

### STOCK-04 — Catálogo sin edición directa de stock

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | `STOCK-02` |
| Bloquea | `STOCK-06` |
| Etiquetas | `stock`, `catalogo`, `backend`, `frontend` |

**Objetivo:** cerrar la única vía que hoy saltea `StockService`.

**Alcance:**

- Quitar `stockActual` de `ProductoUpdateDTO` e `InsumoUpdateDTO` y de los formularios de
  edición de `ProductosPage` e `InsumosPage`.
- **Decisión confirmada (opción A):** en el alta se mantiene el campo de stock inicial y se
  registra como movimiento `INICIAL` a través de `StockService` (inyección `@Lazy` en
  `ProductoService`/`InsumoService` para romper el ciclo de dependencias). El formulario de alta
  no cambia respecto de hoy.
- El alta con saldo insuficiente no aplica: `INICIAL` siempre es `ENTRADA`.

**Criterios de aceptación:**

- [ ] Editar un producto/insumo ya no cambia su `stock_actual`.
- [ ] Dar de alta con stock genera un movimiento `INICIAL` auditable.
- [ ] `./mvnw compile` y `npm run build` pasan.

---

### STOCK-05 — Pantallas de stock

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | `STOCK-03` |
| Bloquea | `STOCK-07` |
| Etiquetas | `stock`, `frontend` |

**Objetivo:** ver y ajustar saldos desde la UI.

**Alcance:**

- `StockInsumosPage.jsx` en `/stock/insumos`: tabla con nombre, unidad, saldo, mínimo y estado
  (normal / bajo mínimo), filtro por nombre, botón “Registrar movimiento” y ver kardex.
- `StockProductosPage.jsx` en `/stock/productos`: tabla con nombre, categoría y saldo, filtro
  por nombre, movimiento y kardex.
- `stockService.js` con las llamadas a la API.
- Dos links en `Navbar.jsx`, visibles para administradores y vendedores; rutas dentro de
  `RutaProtegida` (sin `soloAdmin`).

**Criterios de aceptación:**

- [ ] Un vendedor ve ambas pantallas y puede registrar entradas y salidas.
- [ ] Una salida mayor al saldo muestra el error `409` y no cambia la tabla.
- [ ] El kardex muestra `fecha_hora`, tipo, motivo, cantidad y usuario.
- [ ] `npm run lint` y `npm run build` pasan.

---

### STOCK-06 — Tests

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | `STOCK-02`, `STOCK-03`, `STOCK-04` |
| Bloquea | — |
| Etiquetas | `stock`, `tests` |

**Alcance:**

- `StockServiceTest`: entrada, salida, saldo insuficiente con rollback, usuario de la sesión,
  rechazo de producto+insumo a la vez, rechazo de cantidad no positiva.
- `MovimientoStockRepositoryTest` con Testcontainers: filtros y orden del historial.
- `StockControllerTest` con MockMvc: `201`, `200`, `400`, `404`, `409`, `401`.
- Comprobación de que catálogo ya no rescribe `stock_actual`.

**Criterios de aceptación:**

- [ ] `./mvnw verify` pasa con Docker/PostgreSQL disponible.
- [ ] `npm run lint` y `npm run build` pasan.

---

### STOCK-07 — Documentación y Swagger

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | `STOCK-01`, `STOCK-03`, `STOCK-05` |
| Bloquea | — |
| Etiquetas | `stock`, `docs` |

**Alcance:**

- Interfaz `StockApi` con `@Tag`, `@Operation`, `@ApiResponse` y `ErrorResponse`, igual que
  `CajaApi`.
- Documentar `Movimiento_Stock` en `docs/modelo-datos.md` y sacarlo de “Tablas pendientes”.
- Agregar endpoints y rutas en `docs/arquitectura.md`, y el módulo en `README.md`.
- Agregar este backlog al mapa de `docs/tickets/README.md` y actualizar los estados de los
  tickets a medida que se cierren.
- Registrar los conflictos con `VEN-10`/`INC-07`/`CAJA-07` en `preguntas.md`.

**Criterios de aceptación:**

- [ ] Swagger UI muestra el tag **Stock** con el alcance de esta tanda.
- [ ] La documentación refleja lo implementado, no lo planeado.

## Verificación del módulo

```bash
cd backend
./mvnw verify
```

```bash
cd frontend
npm run lint
npm run build
```

Prueba manual en base limpia:

1. Iniciar sesión como vendedor y abrir `/stock/insumos` y `/stock/productos`: ambas cargan.
2. Registrar una entrada de stock y verificar que el saldo aumenta y aparece en el kardex.
3. Registrar una salida mayor al saldo: debe dar `409` y no modificar nada.
4. Editar un producto/insumo desde catálogo: ya no debe tener campo de stock.
5. Entrar como administrador y repetir las consultas.
6. Sin sesión, llamar a `/api/stock/movimientos`: debe dar `401`.

## Fuera de alcance de esta tanda

- Descuento de stock al confirmar ventas (`VEN-10`).
- Descuento al cerrar incidencias (`INC-07`).
- Ingresos de mercadería y generación automática del costo (`CST-06`).
- Pedidos, auditoría, reportes y stock negativo.

## Conflictos registrados

- `VEN-10` tenía la decisión 8b de permitir stock negativo al confirmar ventas. Se revirtió el
  26/11/2026: ahora exige `409` sin movimiento, alineado con este módulo. Ya no hay conflicto
  pendiente.
- `INC-07` exige `409` con stock insuficiente: es compatible con la regla de este módulo.
- `CAJA-07` quedó resuelto el 26/11/2026 con devolución condicional: al anular se buscan las
  `SALIDA` con `id_venta` y se genera una `ENTRADA`/`DEVOLUCION` por cada una. Como `VEN-10`
  todavía no descuenta, hoy no se mueve stock; cuando se implemente, debe registrar sus salidas
  con la FK `id_venta` para que la devolución funcione sin cambios.

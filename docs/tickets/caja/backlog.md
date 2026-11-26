# Backlog — Módulo Caja

Tickets del módulo `caja` escritos como rebanadas verticales. La **Fase 1** implementa la
caja operativa sin ventas ni correcciones: abrir, registrar egresos, ver la sesión actual,
cerrar, revisar diferencias y consultar el historial. Las integraciones con ventas y costos,
la corrección de cierres y la auditoría quedan para fases posteriores con su bloqueante
explícito.

**Fuentes de verdad:** [`caja-decisiones.md`](../../caja/caja-decisiones.md),
[`GLOSSARY.md`](../../especificaciones/GLOSSARY.md), los ADR
[`0001`](../../caja/adr/0001-caja-como-sesion-con-movimientos.md) y
[`0002`](../../caja/adr/0002-anulacion-por-contramovimiento.md), y los tickets CAJA-01 a
CAJA-09 del PDF del proyecto. El export de Vertabelo está atrasado: no tiene
`Cierre_Caja_Detalle` ni `Movimiento_Caja.id_costo`, y trae una columna artefacto
`Sesion_Caja.Usuario_id_usuario`. Se ignora el artefacto y se agregan las dos piezas que
faltan.

## Decisiones vigentes

- **Una sola caja compartida.** Nunca hay dos sesiones `ABIERTA` al mismo tiempo. No existe
  tabla `Caja`.
- **Abre y cierra cualquier usuario autenticado.** No hace falta ser quien abrió. El
  administrador es el único que revisa sesiones con diferencia.
- **Una sesión `PENDIENTE_REVISION` ya está cerrada para operar.** No impide abrir la
  siguiente.
- **Medios de pago:** `EFECTIVO` y `TRANSFERENCIA`. No hay tabla de medios de pago.
- **Diferencia = total declarado − total esperado.** Negativa es faltante, positiva es
  sobrante.
- **El monto inicial es efectivo.** El sistema sugiere el total declarado en efectivo del
  último cierre (incluye sesiones `PENDIENTE_REVISION`). Si quien abre lo cambia, el motivo
  es obligatorio.
- **Los movimientos no se editan ni se borran.** En Fase 1 solo se crean egresos por
  `RETIRO` o `GASTO`; los ingresos por venta se registran con CAJA-02 y las anulaciones se
  registran con CAJA-07 como contramovimientos.
- **Un egreso en efectivo no puede superar el efectivo esperado en ese momento.** Los
  egresos por transferencia no tienen ese límite.
- **Al cerrar se declara cada medio por separado.** Siempre se crean dos filas de detalle:
  `EFECTIVO` y `TRANSFERENCIA`, aunque alguna quede en cero.
- **Si todas las diferencias son cero → `CERRADA`.** Si alguna no es cero →
  `PENDIENTE_REVISION` y la observación del cierre es obligatoria.
- **La revisión no cambia montos.** Solo guarda quién, cuándo y la observación, y pasa la
  sesión a `CERRADA`. Para cambiar montos está CAJA-08 (postergado).
- **Enums en mayúscula en código, base y API.** La UI muestra etiquetas legibles:
  Abierta, Pendiente de revisión, Cerrada, Efectivo, Transferencia, Retiro, Gasto, etc.
- **Usuario desde la sesión.** El backend usa `UsuarioService.obtenerAutenticado()`; nunca
  acepta `id_usuario` desde el frontend.
- **Sin Flyway en Fase 1.** Se mantiene `spring.jpa.hibernate.ddl-auto=update`. La unicidad
  de la sesión abierta se protege con un advisory lock transaccional de PostgreSQL
  (`pg_advisory_xact_lock`). Si más adelante se activan migraciones, hay que baselinear.

**Convención de nombres:** los identificadores (`id_sesion`, `id_usuario_apertura`,
`id_movimiento_caja`, etc.) van en `snake_case`; el resto de campos, métodos y claves JSON
van en `camelCase`. Las columnas de la base conservan `snake_case` mediante
`@Column(name = ...)`.

Leyenda: ✅ Hecho · 🟡 Parcial · 🟢 Listo · 🔴 Bloqueado.

## Resumen

| ID | Ticket | Fase | Bloqueado por | Estado |
|---|---|---|---|---|
| [CAJA-01](#caja-01-abrir-la-caja) | Abrir la caja | 1 | — | ✅ |
| [CAJA-03](#caja-03-registrar-un-egreso-de-caja) | Registrar un egreso de caja | 1 | CAJA-01 | ✅ |
| [CAJA-04](#caja-04-ver-la-caja-abierta) | Ver la caja abierta | 1 | CAJA-01, CAJA-03 | 🟡 |
| [CAJA-05](#caja-05-cerrar-la-caja) | Cerrar la caja | 1 | CAJA-04 | ✅ |
| [CAJA-06](#caja-06-revisar-una-sesión-con-diferencia) | Revisar una sesión con diferencia | 1 | CAJA-05 | ✅ |
| [CAJA-09](#caja-09-historial-de-sesiones-de-caja) | Historial de sesiones | 1 | CAJA-05 | 🟡 |
| [CAJA-02](#caja-02-cobrar-una-venta-con-uno-o-varios-medios-de-pago) | Cobrar una venta | 2 | — | ✅ |
| [CAJA-07](#caja-07-anular-una-venta) | Anular una venta | 2 | — | ✅ |
| [CAJA-08](#caja-08-corregir-un-cierre) | Corregir un cierre | 3 | CAJA-05, auditoría | 🔴 |

## Fase 1

### CAJA-01. Abrir la caja

**Historia:** como vendedor o administrador, quiero abrir una sesión de caja declarando el
monto inicial, para empezar a operar.

**Qué construir**

- `POST /api/caja/sesiones` con `montoInicial` y `motivoAjusteInicial` opcional.
- `GET /api/caja/actual` devuelve la sesión abierta (o `null`) y el monto inicial sugerido.
- Si ya hay una sesión `ABIERTA`, responde `409` con quién la abrió y cuándo.
- Si el monto ingresado difiere del sugerido, el motivo es obligatorio.
- Guarda `id_usuario_apertura`, fecha y hora del servidor, y estado `ABIERTA`.
- La sugerencia sale del `totalDeclarado` en `EFECTIVO` del último cierre; 0 si no hay.
- Un `PENDIENTE_REVISION` no bloquea la apertura.

**Criterios de aceptación**

- [x] No se puede abrir una segunda sesión mientras haya una `ABIERTA`.
- [x] El monto sugerido es el declarado en efectivo del último cierre (o 0).
- [x] Cambiar el monto sin motivo responde `400`.
- [x] La sesión queda `ABIERTA` con usuario, fecha y hora.
- [x] Dos aperturas concurrentes no pueden crear dos sesiones abiertas.

**Tablas:** `Sesion_Caja`.

### CAJA-03. Registrar un egreso de caja

**Historia:** como usuario, quiero registrar dinero que sale de la caja (un retiro o un gasto
chico), para que el saldo esperado refleje la realidad.

**Qué construir**

- `POST /api/caja/sesiones/{id_sesion}/movimientos`.
- Concepto `RETIRO` o `GASTO`, medio `EFECTIVO` o `TRANSFERENCIA`, monto mayor a 0 y
  observación opcional de hasta 150 caracteres.
- Solo se puede registrar si la sesión está `ABIERTA`.
- Un egreso en efectivo no puede superar el efectivo esperado en ese momento.
- Un egreso por transferencia no tiene límite.
- El movimiento guarda usuario, fecha y hora del servidor.
- El vínculo opcional con un costo (`id_costo`) queda para CST-05: la columna existe pero no
  se expone en la API de Fase 1.

**Criterios de aceptación**

- [x] Sin sesión abierta responde `409`.
- [x] Monto <= 0 responde `400`.
- [x] Concepto `VENTA` o `ANULACION` responde `400` en este endpoint.
- [x] Un egreso en efectivo mayor al esperado responde `409`.
- [x] Un egreso por transferencia se acepta aunque el esperado sea menor.
- [x] El movimiento queda asociado a la sesión y al usuario autenticado.

**Tablas:** `Movimiento_Caja`.

### CAJA-04. Ver la caja abierta

**Historia:** como usuario, quiero ver el estado de la sesión abierta y sus movimientos, para
saber cuánto debería haber en cada medio de pago.

**Qué construir**

- Pantalla `/caja` con la sesión abierta o el formulario de apertura si no hay ninguna.
- Muestra quién abrió, cuándo y el monto inicial.
- Lista los movimientos de la sesión, del más nuevo al más viejo, con tipo, concepto, medio,
  monto, usuario y hora.
- Muestra el esperado por medio:
  - efectivo = monto inicial + ingresos − egresos;
  - transferencia = ingresos − egresos.
- Si no hay sesión abierta, ofrece abrirla con el monto sugerido.

**Criterios de aceptación**

- [x] La pantalla muestra la sesión abierta y sus totales por medio.
- [x] Los movimientos se listan del más nuevo al más viejo.
- [x] Sin sesión abierta se muestra el formulario de apertura.
- [ ] Los ingresos por venta aparecen en la lista (llegan con CAJA-02).

**Tablas:** `Sesion_Caja`, `Movimiento_Caja`.

### CAJA-05. Cerrar la caja

**Historia:** como usuario, quiero cerrar la sesión declarando lo que hay en cada medio de
pago, para comparar contra lo esperado y dejar el cierre registrado.

**Qué construir**

- `POST /api/caja/sesiones/{id_sesion}/cierre` con `declaradoEfectivo`,
  `declaradoTransferencia` y observación.
- El declarado por transferencia es el neto de la sesión (cobros − pagos) y puede ser
  negativo. El declarado en efectivo no puede ser negativo.
- Muestra el esperado por medio y pide el declarado de cada uno.
- Crea una fila de `Cierre_Caja_Detalle` por medio, con esperado, declarado y diferencia.
- Guarda usuario, fecha y hora de cierre, y los totales globales (suma del detalle).
- Si todas las diferencias son 0 → `CERRADA`.
- Si alguna diferencia no es 0 → `PENDIENTE_REVISION` y observación obligatoria.
- Después de cerrar no se pueden registrar más movimientos.
- Cualquier usuario autenticado puede cerrar, no solo quien abrió.

**Criterios de aceptación**

- [x] Se crean siempre dos filas de detalle, una por medio.
- [x] La diferencia se calcula como declarado − esperado.
- [x] Sin diferencia la sesión queda `CERRADA`.
- [x] Con diferencia y sin observación responde `400`.
- [x] Con diferencia y observación queda `PENDIENTE_REVISION`.
- [x] Un neto por transferencia negativo se acepta y se compara contra el esperado negativo.
- [x] Un cierre concurrente no duplica detalles ni cambia una sesión ya cerrada.
- [x] No se aceptan movimientos en una sesión cerrada.

**Tablas:** `Sesion_Caja`, `Cierre_Caja_Detalle`.

### CAJA-06. Revisar una sesión con diferencia

**Historia:** como administrador, quiero revisar las sesiones pendientes y dejar constancia,
para cerrar el circuito sin cambiar los montos declarados.

**Qué construir**

- Pantalla `/caja/revision`, solo para administradores.
- Lista las sesiones `PENDIENTE_REVISION` con fecha, quién cerró y la diferencia por medio.
- `POST /api/caja/sesiones/{id_sesion}/revision` con observación obligatoria.
- Guarda `id_usuario_revision`, `fechaRevision` (fecha y hora) y `observacionRevision`.
- Pasa la sesión a `CERRADA`; los montos no cambian.

**Criterios de aceptación**

- [x] Solo un administrador puede ejecutar la revisión (`403` para vendedores).
- [x] Una sesión que no está `PENDIENTE_REVISION` responde `409`.
- [x] La observación es obligatoria.
- [x] La revisión guarda usuario, fecha y hora.
- [x] Los totales y detalles del cierre no se modifican.

**Tablas:** `Sesion_Caja`.

### CAJA-09. Historial de sesiones de caja

**Historia:** como usuario, quiero consultar las sesiones anteriores con filtros, para
auditar la operación de la caja.

**Qué construir**

- Pantalla `/caja/historial`.
- `GET /api/caja/sesiones` con filtros `desde`, `hasta`, `estado` y `id_usuario`.
- El filtro por usuario matchea **apertura o cierre**.
- Cada sesión muestra apertura, cierre, quién abrió, quién cerró, monto inicial, esperado,
  declarado y diferencia por medio.
- Al abrir una sesión se ven sus movimientos y, si las hubo, su revisión.
- Los vendedores ven las sesiones; los administradores además ven las correcciones.

**Criterios de aceptación**

- [x] Filtra por rango de fechas de apertura, estado y usuario.
- [x] El filtro por usuario incluye sesiones abiertas o cerradas por esa persona.
- [x] El detalle muestra los movimientos y los totales por medio.
- [x] El detalle muestra la revisión cuando existe.
- [ ] Las correcciones de cierre se muestran (llegan con CAJA-08).

**Tablas:** `Sesion_Caja`, `Movimiento_Caja`, `Cierre_Caja_Detalle`.

## Fase 2

### CAJA-02. Cobrar una venta con uno o varios medios de pago

**Estado:** ✅ Hecho.

- [x] Al confirmar una venta, exigir una sesión `ABIERTA` (bloqueada con
  `PESSIMISTIC_WRITE` para que no se cierre durante el cobro).
- [x] Cargar uno o más pagos con medio (`EFECTIVO` / `TRANSFERENCIA`) y monto; la suma debe
  ser igual al total de la venta calculado por el servidor.
- [x] Crear un `Movimiento_Caja` `INGRESO`/`VENTA` por cada pago, con `id_venta`, el usuario
  autenticado y la fecha/hora de la venta.
- [x] Guardar venta y movimientos en una sola transacción.
- [x] `Venta` no tiene `medio_pago`: los pagos se consultan desde los movimientos de caja y
  se devuelven en `VentaResponseDTO.pagos`.
- [x] `VentasPage` muestra la caja abierta, permite pagos mixtos y deshabilita el alta sin caja.

**Archivos:** `CobroVentaDTO`, `VentaCreateDTO`, `VentaResponseDTO`, `VentaService`,
`MovimientoCajaRepository`, `VentasPage`, tests de servicio y controlador de ventas.

### CAJA-07. Anular una venta

**Estado:** ✅ Hecho el 26/11/2026.

- [x] Exigir una sesión `ABIERTA` (puede ser distinta de la que cobró). Si no hay, responde `409`.
- [x] Pedir motivo obligatorio (`AnularVentaDTO.motivo`, hasta 150 caracteres).
- [x] Pasar la venta a `ANULADA` sin borrar nada: la operación es una sola transacción, así que
  si falla caja o stock la venta sigue `CONFIRMADA`.
- [x] Por cada ingreso `VENTA` de la venta, crear un `EGRESO`/`ANULACION` del mismo monto y medio,
  con el `id_venta` original, usuario y fecha/hora actuales y el motivo en `observaciones`.
- [x] No tocar los movimientos originales ni las sesiones cerradas: los contramovimientos van a
  la sesión abierta en el momento de la anulación (ADR 0002).
- [x] Limitar la devolución en efectivo al efectivo disponible en esa sesión (`409` si alcanza
  menos). Las transferencias no tienen ese límite, igual que en CAJA-03.
- [x] Devolver stock con un `Movimiento_Stock` `ENTRADA`/`DEVOLUCION` **por cada `SALIDA` que la
  venta haya registrado con su `id_venta`**, vinculado a la misma venta. Hoy VEN-10 todavía no
  descuenta stock, por lo que no se genera stock de la nada; cuando se implemente, la devolución
  empieza a funcionar sin cambios en este ticket.
- [ ] Registrar en auditoría (postergado junto con el módulo `auditoria`).

**Archivos:** `AnularVentaDTO`, `CajaService.anularCobrosDeVenta`,
`StockService.devolverStockDeVenta`, `MovimientoStockRepository.listarPorVentaYTipo`,
`VentaService.anularVenta`, `VentaController`, `ventasService.anular`, `VentasPage` y tests.

## Fase 3

### CAJA-08. Corregir un cierre

**Estado:** 🔴 Bloqueado por CAJA-05 y por el módulo de auditoría.

- Solo administradores.
- Corregir el declarado de cada medio y la observación.
- Recalcular diferencias y totales.
- No editar movimientos.
- Guardar en auditoría usuario, fecha, hora, registro, valor anterior y valor nuevo.
- Motivo obligatorio.
- Decidir si el desvínculo de un costo con un egreso se resuelve por acá (CST-05).

## Modelo de datos de Fase 1

### `Sesion_Caja`

| Columna | Tipo | Restricciones |
|---|---|---|
| `id_sesion` | `bigint` | PK, autoincremental |
| `id_usuario_apertura` | `bigint` | FK → `Usuario`, not null |
| `id_usuario_cierre` | `bigint` | FK → `Usuario`, nullable |
| `id_usuario_revision` | `bigint` | FK → `Usuario`, nullable |
| `fecha_apertura` | `date` | not null |
| `hora_apertura` | `time` | not null |
| `fecha_cierre` | `date` | nullable |
| `hora_cierre` | `time` | nullable |
| `monto_inicial` | `numeric(12,2)` | not null |
| `motivo_ajuste_inicial` | `varchar(150)` | nullable |
| `estado` | `varchar(30)` | `ABIERTA`, `PENDIENTE_REVISION`, `CERRADA` |
| `total_esperado` | `numeric(12,2)` | nullable hasta cerrar |
| `total_declarado` | `numeric(12,2)` | nullable hasta cerrar |
| `diferencia` | `numeric(12,2)` | nullable hasta cerrar |
| `observaciones` | `varchar(100)` | nullable; obligatoria si hay diferencia |
| `fecha_revision` | `timestamp` | nullable |
| `observacion_revision` | `varchar(150)` | nullable |

### `Movimiento_Caja`

| Columna | Tipo | Restricciones |
|---|---|---|
| `id_movimiento_caja` | `bigint` | PK, autoincremental |
| `id_sesion_caja` | `bigint` | FK → `Sesion_Caja`, not null |
| `id_usuario` | `bigint` | FK → `Usuario`, not null |
| `id_venta` | `bigint` | nullable; se usa en Fase 2 |
| `id_costo` | `bigint` | nullable; se usa con CST-05 |
| `tipo` | `varchar(20)` | `INGRESO`, `EGRESO` |
| `concepto` | `varchar(20)` | `VENTA`, `RETIRO`, `GASTO`, `ANULACION` |
| `monto` | `numeric(12,2)` | not null, > 0 |
| `medio_pago` | `varchar(20)` | `EFECTIVO`, `TRANSFERENCIA` |
| `fecha` | `date` | not null |
| `hora` | `time` | not null |
| `observaciones` | `varchar(150)` | nullable |

### `Cierre_Caja_Detalle`

| Columna | Tipo | Restricciones |
|---|---|---|
| `id_sesion` | `bigint` | PK, FK → `Sesion_Caja` |
| `medio_pago` | `varchar(20)` | PK, `EFECTIVO` / `TRANSFERENCIA` |
| `total_esperado` | `numeric(12,2)` | not null |
| `total_declarado` | `numeric(12,2)` | not null |
| `diferencia` | `numeric(12,2)` | not null |

## Endpoints de Fase 1

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| `GET` | `/api/caja/actual` | autenticado | Sesión abierta (o `null`) y monto sugerido |
| `POST` | `/api/caja/sesiones` | autenticado | Abrir una sesión |
| `POST` | `/api/caja/sesiones/{id_sesion}/movimientos` | autenticado | Registrar un egreso |
| `POST` | `/api/caja/sesiones/{id_sesion}/cierre` | autenticado | Cerrar la sesión |
| `POST` | `/api/caja/sesiones/{id_sesion}/revision` | administrador | Revisar una sesión pendiente |
| `GET` | `/api/caja/sesiones` | autenticado | Historial con filtros y paginación |
| `GET` | `/api/caja/sesiones/{id_sesion}` | autenticado | Detalle con movimientos |

## Seguridad

`SecurityConfig` mantiene la regla global actual y agrega, antes de ella:

- `POST /api/caja/sesiones/*/revision` → `ADMINISTRADOR`.
- `/api/caja/**` → autenticado.

Los vendedores pueden abrir, egresar, cerrar, ver el historial y ver el detalle. No pueden
revisar ni ver la pantalla de revisión.

## Pruebas

Estado actual: **45 pruebas de caja en verde** (`CajaServiceTest` 22, `CajaControllerTest` 14,
`CajaRepositoryTest` 9) y **204 pruebas de backend en verde**.

- `CajaServiceTest`: apertura, sugerencia, motivo, límite de efectivo, cierre con y sin
  diferencia (incluido neto por transferencia negativo), revisión y estados inválidos.
- `CajaControllerTest`: códigos HTTP, validación de DTOs (transferencia negativa aceptada,
  efectivo negativo rechazado) y `403` del vendedor en revisión.
- `CajaRepositoryTest` (Testcontainers PostgreSQL): persistencia, suma por tipo/medio,
  clave compuesta de `Cierre_Caja_Detalle`, filtro por usuario apertura/cierre y último
  cierre con fechas nulas ordenadas al final (`NULLS LAST`).

La suite de Maven fija `-Duser.timezone=UTC` en `pom.xml`: PgJDBC 42.7.x envía la zona del
JVM en el paquete de inicio y PostgreSQL no reconoce el alias `America/Buenos_Aires` que usa
Windows.

Verificación:

```bash
cd backend
./mvnw test "-Dtest=CajaServiceTest,CajaControllerTest,CajaRepositoryTest"
```

```bash
cd frontend
npm run lint
npm run build
```

## Fuera de alcance de Fase 1

- Cobro de ventas y movimientos de ingreso (CAJA-02, ✅ implementado en Fase 2).
- Corrección de cierres y auditoría (CAJA-08).
- Vínculo real con costos (CST-05).
- Migraciones Flyway.
- Reportes de caja.

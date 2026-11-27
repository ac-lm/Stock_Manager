# Tickets: módulo Reportes

Fuente: [reportes-decisiones.md](../reportes-decisiones.md), [GLOSSARY.md](../GLOSSARY.md) y el ADR 0004. Reportes no tiene tablas propias: cada ticket es una consulta de solo lectura + su pantalla (filtros, tabla y gráfico) + su exportación.

Cada ticket es una **rebanada vertical** (consulta, backend y pantalla) que se puede probar de punta a punta. "Bloqueado por" dice qué tiene que estar terminado antes; los marcados 🔒 además esperan que se responda otro módulo y no se empiezan hasta tenerlo.

**Requisitos previos:** login con rol Administrador, y ventas cargadas (VEN-01) para probar.

| # | Ticket | Bloqueado por | Estado |
| --- | --- | --- | --- |
| REP-01 | Ventas por período | VEN-01 | ✅ |
| REP-02 | Exportar un reporte a Excel, CSV y PDF | REP-01 | ✅ |
| REP-03 | Ventas por producto y productos más vendidos | REP-01 | ✅ |
| REP-04 | Ventas por cliente y por tipo de cliente | REP-01, CLI-01 | ✅ |
| REP-05 | Ventas por vendedor | REP-01 | ✅ |
| REP-06 | Evolución de ventas y comparación estacional | REP-01 | ✅ |
| REP-07 | Reporte de cierres de caja | REP-01, CAJA-05 | ✅ |
| REP-08 | Stock actual y productos bajo mínimo | REP-01 | ✅ |
| REP-09 | Entradas, salidas y descartes de stock | REP-01 | ✅ |
| REP-10 | Costos por tipo y su evolución | REP-01 | ✅ |
| REP-11 | Resultado del período (ventas menos costos) | REP-01, REP-10 | ✅ |
| REP-12 | Producción: productos elaborados y consumo de insumos | REP-01 | 🔒 Falta respuesta |
| REP-13 | Incidencias y descartes | REP-01 | 🔒 Falta respuesta |
| REP-14 | Facturación | REP-01 | 🔒 Falta respuesta |

---

## REP-01. Ventas por período

**Qué construir:** el administrador elige un rango de fechas y ve cuánto se vendió, cuántas ventas hubo y el ticket promedio, agrupado por día, semana o mes, en una tabla y un gráfico de barras. Es el primer reporte y deja armada la base que usan todos: pantalla de reportes, filtro de fechas, permiso de administrador y gráfico con Recharts.

**Bloqueado por:** Ninguno de Reportes (necesita VEN-01: Registrar una venta)

**Criterios de aceptación**
- [x] Solo los administradores entran a Reportes: el backend responde 403 a cualquier otro rol, no alcanza con ocultar el menú.
- [x] El rango de fechas es obligatorio y las dos fechas se incluyen completas (el 31 cuenta entero).
- [x] Se puede agrupar por día, semana o mes.
- [x] Cada fila muestra el período, el total vendido, la cantidad de ventas y el ticket promedio; abajo, el total del rango.
- [x] Solo cuentan las ventas confirmadas; las anuladas no, aunque se hayan anulado después.
- [x] Los períodos sin ventas aparecen en 0 (se completan en Java).
- [x] Gráfico de barras del total vendido por período.
- [x] La consulta devuelve un DTO (no entidades) y usa SQL que funciona igual en Postgres 15 y 17.
- [x] Test: ventas confirmadas y anuladas en distintos días; el total coincide con la suma de Venta.total de las confirmadas.

**Decisiones aplicadas:** la semana empieza el lunes y su identificador es la fecha del lunes (`2026-09-28`); el mes se identifica `2026-10` en `periodo` y se muestra `oct 2026` en `etiqueta` (la pantalla y las exportaciones usan siempre `etiqueta`); la agrupación diaria tiene un tope de 5 años (semana y mes no). El rango se consulta como `fecha >= desde AND fecha < hasta + 1 día`.

**Tablas:** Venta (solo lectura)

---

## REP-02. Exportar un reporte a Excel, CSV y PDF

**Qué construir:** desde cualquier reporte, el administrador descarga exactamente lo que está viendo (con los filtros aplicados) en Excel, CSV o PDF. Se prueba con Ventas por período y queda reutilizable para el resto.

**Bloqueado por:** 01: Ventas por período

**Criterios de aceptación**
- [x] Botones Excel, CSV y PDF en la pantalla del reporte.
- [x] El archivo se genera en el backend con la misma consulta y los mismos filtros que la pantalla.
- [x] Excel con encabezados, montos como números (no texto) y una fila de total.
- [x] CSV en UTF-8, con separador punto y coma, para que Excel en español lo abra bien.
- [x] PDF con título del reporte, rango de fechas, fecha de emisión y la tabla.
- [x] El nombre del archivo lleva el reporte y el rango, por ejemplo ventas-por-periodo_2026-10-01_2026-10-31.xlsx.
- [x] La exportación es una pieza común: agregarla a otro reporte no requiere escribirla de nuevo.
- [x] Solo administradores.

**Decisiones aplicadas:** un único endpoint `GET /api/reportes/exportar?reporte=...&formato=xlsx|csv|pdf&...`. Cada reporte registra un proveedor que devuelve el mismo DTO de la pantalla; los generadores de Excel (POI), CSV y PDF (OpenPDF) solo conocen el contrato `ReporteExportable`. La fecha de emisión se muestra en `America/Argentina/Buenos_Aires` como `dd/MM/yyyy HH:mm`, sin sufijo de zona. El CSV usa BOM UTF-8, `;` y coma decimal. Excel limpia los `ThreadLocal` de POI y XmlBeans al terminar.

**Tablas:** Ninguna (usa el DTO del reporte)

---

## REP-03. Ventas por producto y productos más vendidos

**Qué construir:** el administrador ve, para un período, cuánto se vendió de cada producto (en pesos y en cantidad) y el ranking de los más vendidos. Cubre del PDF: ventas por producto, cantidades vendidas, ventas por unidad, ventas por peso y productos más vendidos.

**Bloqueado por:** 01: Ventas por período

**Criterios de aceptación**
- [x] Filtros: rango de fechas (obligatorio), producto y categoría.
- [x] Cada fila muestra producto, categoría, cantidad vendida y total vendido (sin unidad de venta; ver pendiente abajo).
- [x] El total vendido sale de la suma de Detalle_Venta.subtotal de las ventas confirmadas, no del precio actual del catálogo.
- [ ] Las cantidades se suman solo dentro de la misma unidad: nunca se suman unidades con kilos. **Pendiente:** no existe el campo de unidad de venta (ver decisión abajo).
- [ ] Se puede ver el total separado entre productos que se venden por unidad y por peso. **Pendiente:** idem.
- [x] Ranking de los N más vendidos por total vendido (N elegible, por defecto 10), con gráfico de barras horizontales.
- [x] Exporta con lo de REP-02.

**Decisiones aplicadas:** la categoría se toma de `Producto.categoria` (no hay entidad ni tabla Categoría). El ranking ordena por total vendido y desempata por nombre; el tope N lo elige el usuario en la pantalla (5, 10, 20 o 50; por defecto 10) y no achica la tabla, que muestra todos los productos con ventas. La exportación genera la tabla completa con el contrato de REP-02; el ranking es un subconjunto ordenado de esas mismas filas y se describe en el encabezado del archivo (`Top N por total vendido`). Identificador de exportación: `ventas-por-producto`.

**Pendiente documentado (unidad de venta):** el ticket pedía mostrar la unidad de venta y separar los totales entre productos que se venden por unidad y por peso. Hoy `Producto` no tiene un campo `unidad_venta`: la decisión de negocio dice que cada producto se vende de una sola forma, pero ese dato nunca se guardó en la base. Por decisión de Mayra (26/11/2026) se omite ese desglose en esta entrega y queda pendiente hasta que Catálogo agregue la columna con su migración. No se infiere de los decimales de `Detalle_Venta.cantidad`, porque un kilo vendido en número entero daría un resultado falso.

**Tablas:** Detalle_Venta, Venta, Producto (solo lectura). No hay tabla Categoría: la categoría es `Producto.categoria`.

---

## REP-04. Ventas por cliente y por tipo de cliente

**Qué construir:** el administrador ve cuánto le compró cada cliente en un período, y el total por tipo de cliente (gran cliente y consumidor final).

**Bloqueado por:** 01: Ventas por período; CLI-01: Registrar un cliente

**Criterios de aceptación**
- [x] Filtros: rango de fechas (obligatorio), cliente y tipo de cliente.
- [x] El filtro de cliente permite elegir también clientes inactivos.
- [x] Cada fila muestra cliente, tipo, cantidad de compras y total vendido, ordenado de mayor a menor.
- [x] Solo cuentan las ventas confirmadas; las anuladas no, aunque se hayan anulado después.
- [x] Las ventas de mostrador aparecen en el cliente genérico "Consumidor final" como un cliente más.
- [x] Una vista resumida muestra el total por tipo de cliente, con gráfico de barras.
- [x] Exporta con lo de REP-02 (Excel, CSV y PDF, con los mismos filtros).
- [x] Solo administradores: el backend responde 403 al vendedor.

**Decisiones aplicadas:** el rango es inclusivo (`fecha >= desde AND fecha < hasta + 1 día`) y el total sale de `SUM(Venta.total)`, igual que REP‑01. La "cantidad de compras" es `COUNT` de ventas confirmadas. El orden es por total vendido descendente y desempata por nombre de cliente. `GENERICO` se unifica con `CONSUMIDOR_FINAL` en el filtro y en el resumen (una sola barra "Consumidor final"), por decisión de Mayra (27/11/2026); en la tabla el genérico mantiene su propia fila con el nombre "Consumidor final (mostrador)". El resumen se calcula sobre las filas ya filtradas y siempre muestra los dos tipos, aunque estén en cero. El selector de clientes carga la lista completa con `incluirInactivos=true` (sin paginar, porque el taller tiene pocos clientes) y marca "(inactivo)" a los dados de baja. La exportación genera solo la tabla por cliente, con los filtros en el encabezado; identificador: `ventas-por-cliente`.

**Tablas:** Venta, Cliente (solo lectura). No se agregaron tablas ni columnas. Si apareciera una venta con `id_cliente` nulo en la base, queda fuera de este reporte (no se modifica) y hay que avisar: el modelo la declara obligatoria.

---

## REP-05. Ventas por vendedor

**Qué construir:** el administrador ve cuánto vendió cada vendedor en un período. Vendedor es el usuario que registró la venta.

**Bloqueado por:** 01: Ventas por período

**Criterios de aceptación**
- [x] Filtros: rango de fechas (obligatorio e inclusivo) y vendedor opcional, incluyendo usuarios inactivos.
- [x] Cada fila muestra vendedor, cantidad de ventas, total vendido y ticket promedio, con gráfico de barras.
- [x] Solo aparecen los vendedores con ventas confirmadas en el rango, ordenados de mayor a menor total, con fila de totales abajo.
- [x] Solo cuentan las ventas confirmadas; las anuladas no, aunque se hayan anulado después.
- [x] Incluye a los administradores si registraron ventas.
- [x] La etiqueta del vendedor es `nombre (rol)`; si dos usuarios tienen el mismo nombre, lleva también el email.
- [x] Exporta con lo de REP-02 (Excel, CSV y PDF, con los mismos filtros).
- [x] Solo administradores: el backend responde 403 al vendedor.

**Decisiones aplicadas:** Ventas 10 ya está respondida con la opción a) "siempre el usuario que carga la venta" (`id_vendedor` se toma de `UsuarioService.obtenerAutenticado()`), así que la consulta usa `Venta.id_vendedor` sin cambios de esquema. El rango es inclusivo (`fecha >= desde AND fecha < hasta + 1 día`) y el total sale de `SUM(Venta.total)`; la cantidad es `COUNT` de ventas confirmadas. El ticket promedio es `total vendido / cantidad de ventas` con dos decimales (0 si no hubo ventas), tanto por fila como en los totales. El orden desempata por nombre de usuario. La etiqueta muestra el rol en mayúsculas del enum (`VENDEDOR`/`ADMINISTRADOR`) y, cuando dos usuarios comparten nombre entre las filas del reporte, se agrega el email para distinguirlos; la baja lógica no cambia el historial. El selector carga la lista completa de usuarios con `incluirInactivos=true` (sin paginar, igual que en REP‑04) y marca "(inactivo)" a los dados de baja. La exportación genera solo la tabla por vendedor, con los filtros en el encabezado; identificador: `ventas-por-vendedor`.

**Tablas:** Venta, Usuario (solo lectura). No se agregaron tablas ni columnas. Consulta 0 ventas con `id_vendedor` nulo y 0 huérfanas en la base de desarrollo; si apareciera alguna, queda fuera del reporte sin modificarla.

---

## REP-06. Evolución de ventas y comparación estacional

**Qué construir:** el administrador ve cómo evolucionan las ventas mes a mes y compara cada mes con el mismo mes de años anteriores, con una línea por año en el mismo gráfico.

**Bloqueado por:** 01: Ventas por período

**Criterios de aceptación**
- [x] Filtros: años a comparar (por defecto el actual y los dos anteriores), producto y categoría.
- [x] Gráfico de líneas superpuestas: eje X de enero a diciembre, una línea por año.
- [x] Tabla con meses en filas y años en columnas, más la variación porcentual contra el año anterior.
- [x] Los meses sin ventas valen 0; los meses que todavía no pasaron no se dibujan.
- [x] Agrupa con EXTRACT(YEAR) y EXTRACT(MONTH) sobre la fecha de la venta, solo confirmadas.
- [x] Exporta con lo de REP-02 (Excel, CSV y PDF, con los mismos filtros).
- [x] Solo administradores: el backend responde 403 al vendedor.

**Decisiones aplicadas:** por omisión se comparan el año actual y los dos anteriores; se pueden elegir de 1 a 5 años, sin repetir, entre 2000 y el año actual, y el DTO los devuelve ordenados de menor a mayor. Sin filtro de producto se suma `Venta.total` (como REP‑01); con producto o categoría se suma `Detalle_Venta.subtotal` de las líneas que cumplen (como REP‑03), con la categoría tomada de `Producto.categoria`. El eje X va de enero a diciembre del primer año completo; en el año en curso se omiten los meses futuros, tanto en la tabla como en el gráfico. "Hoy" se determina en el backend con un `Clock` en `America/Argentina/Buenos_Aires` (inyectable en los tests) y en la pantalla con la misma zona. Cada año se compara contra el inmediatamente anterior dentro de los seleccionados, mes por mes y también en los totales: si el año anterior vendió 0 se muestra `—` (no se divide por cero). La variación es `(actual - anterior) / anterior * 100`, con dos decimales. Identificador de exportación: `evolucion-ventas`; nombre de archivo `evolucion-ventas_<desde>_<hasta>.<ext>`.

**Tablas:** Venta, Detalle_Venta y Producto (solo lectura). No se agregaron tablas ni columnas.

---

## REP-07. Reporte de cierres de caja

**Qué construir:** el administrador ve para un período los cierres de caja con sus totales por medio de pago y las diferencias, y cuánto faltó o sobró en total. CAJA-09 es el historial para consultar una sesión; este reporte suma y grafica.

**Bloqueado por:** 01: Ventas por período; CAJA-05: Cerrar la caja

**Criterios de aceptación**
- [x] Filtros: rango de fechas (obligatorio), usuario que cerró, estado de la sesión y medio de pago.
- [x] Cada fila muestra la sesión: fecha y hora de apertura y cierre, quién abrió y quién cerró, monto inicial, esperado, declarado y diferencia por medio de pago, y estado.
- [x] Totales del período por medio de pago: ingresos, egresos, faltantes y sobrantes.
- [x] Resalta las sesiones en pendiente de revisión.
- [ ] Si un cierre tuvo una corrección, muestra el valor vigente y marca que fue corregido. **Pendiente:** hoy se muestra el valor vigente, pero no hay marca de "corregido" (ver pendiente abajo).
- [x] Gráfico de barras de la diferencia de cada sesión (negativas y positivas).
- [x] Exporta con lo de REP-02 (Excel, CSV y PDF, con los mismos filtros).
- [x] Solo administradores: el backend responde 403 al vendedor.

**Decisiones aplicadas (Mayra, 27/11/2026):** el período se filtra por `Sesion_Caja.fecha_apertura`, con fechas inclusivas, y solo entran `CERRADA` y `PENDIENTE_REVISION`; las `ABIERTA` quedan fuera porque no tienen cierre ni declarado. Esperado, declarado y diferencia se toman de los valores vigentes en `Cierre_Caja_Detalle` (`diferencia = declarado - esperado`, con dos decimales). Ingresos y egresos salen de `Movimiento_Caja` agrupados por `tipo` y `medio_pago`; `ANULACION` se registró como `EGRESO`, así que cuenta como egreso y **no se filtra por `Venta.estado`** (ADR 0002: la anulación se contabiliza en la sesión en que se hizo, no se reescribe la sesión original). El monto inicial no cuenta como ingreso ni egreso. Faltante es la diferencia negativa en valor absoluto y sobrante la positiva; se suman por separado, no se netean. El selector de usuario incluye inactivos (se cargan todos, sin paginar) y filtra por quien cerró (`id_usuario_cierre`). El filtro de medio de pago acota las columnas y los totales, pero **no saca sesiones**: las que no tienen ese medio muestran cero. La exportación reutiliza REP‑02 con id `cierres-caja` y nombre `cierres-caja_<desde>_<hasta>.<ext>`; lleva la tabla con fila de totales y no incluye el gráfico. El gráfico de la pantalla muestra una barra por sesión: faltantes hacia abajo (rojo) y sobrantes hacia arriba (verde), con las pendientes de revisión resaltadas también en la tabla.

**Pendiente documentado (marca de "corregido"):** CAJA‑08 (corregir un cierre) todavía no está implementada: no hay flag, fecha, usuario ni endpoint de corrección, y `CORRECCION_CAJA` existe en `AccionAuditoria` pero no se usa. Por decisión de Mayra (27/11/2026) el reporte muestra el valor vigente guardado en `Cierre_Caja_Detalle` y **no infiere correcciones desde Auditoría**. Cuando CAJA‑08 agregue el dato, se podrá marcar la fila como corregida sin tocar este reporte.

**Tablas:** Sesion_Caja, Cierre_Caja_Detalle, Movimiento_Caja, Usuario (solo lectura). No se agregaron tablas ni columnas; no se modificó el módulo Caja.

---

## REP-08. Stock actual y productos bajo mínimo ✅

**Qué construir:** el administrador ve el stock actual de productos e insumos y, en especial, los que están bajo mínimo.

**Bloqueado por:** 01: Ventas por período

**Criterios de aceptación**
- [x] De dónde sale el stock actual: columna `stock_actual` en `Producto`/`Insumo`, con `COALESCE` a 0; no se suman movimientos.
- [x] Filtros: tipo (producto o insumo), categoría y "solo bajo mínimo".
- [x] Cada fila muestra artículo, unidad, stock actual, stock mínimo y cuánto falta para llegar al mínimo.
- [x] Gráfico de barras stock actual contra mínimo para los bajo mínimo.
- [x] No lleva rango de fechas: es una foto de hoy.
- [x] Exporta con lo de REP-02 (Excel, CSV y PDF, con los mismos filtros).
- [x] Solo administradores: el backend responde 403 al vendedor.

**Respuesta de Stock (revisión solo lectura, 27/11/2026):** el saldo vigente es la columna denormalizada `stock_actual` (`numeric(12,3)`, `not null`) de `Producto` e `Insumo` (`Producto.java:32`, `Insumo.java:29`). `StockService` es el único que la escribe y lo hace junto con el `Movimiento_Stock` en la misma transacción; los movimientos son inmutables (`ENTRADA`/`SALIDA`, sin estado ni flag de anulación) y no hay reconciliación entre saldo y movimientos. El mínimo existe solo en `Insumo.stock_minimo` (`Insumo.java:35`); `Producto` no lo tiene. La categoría es `Producto.categoria` (`String` libre, obligatoria) y los insumos no tienen categoría. La unidad existe solo en `Insumo.unidad_medida`; no hay `unidad_venta` ni conversión entre unidades.

**Decisiones aplicadas (Mayra, 27/11/2026):** es una foto sin rango de fechas: el día se toma del `Clock` del taller (`America/Argentina/Buenos_Aires`, inyectable en tests). Un insumo con mínimo `null` usa `0`, igual que `StockService`, y "bajo mínimo" es `stock < mínimo` estricto. Los productos no tienen mínimo ni unidad: se muestra `—`/N/A, nunca se marcan como "bajo mínimo" y quedan como pendiente (ver abajo). Los insumos no tienen categoría: con "Todas" aparecen con `—`; al elegir una categoría se filtran solo los productos, se ocultan los insumos y la pantalla lo avisa. La tabla incluye `Tipo` y `Categoría` para que se entienda el filtro, ordenada por tipo y nombre. "Falta para el mínimo" es `max(mínimo − stock, 0)`: `0` cuando no falta y `—` si el mínimo es N/A; el gráfico solo incluye los bajo mínimo y los ordena por mayor falta. No se suman stocks de unidades distintas. La exportación reutiliza REP‑02 con id `stock-actual`: como el contrato compartido exige `desde()`/`hasta()`, ambos valen hoy y el nombre es `stock-actual_<hoy>_<hoy>.<ext>`; la línea "Foto al …" va en la descripción de filtros, junto a "Desde/Hasta" que REP‑02 siempre imprime. La fila de totales del archivo no suma stocks: solo indica la cantidad de artículos; los valores N/A numéricos quedan vacíos en el archivo (en pantalla se muestra `—`).

**Pendiente documentado (mínimo y unidad de Producto):** igual que en REP‑03, `Producto` no tiene `stock_minimo` ni `unidad_venta`. No se infiere el mínimo desde los movimientos ni se agrega la columna desde Reportes: se muestra N/A y el artículo nunca entra en "bajo mínimo". Cuando Catálogo agregue ambos campos con su migración, se podrán completar la columna de mínimo, la de unidad y el cálculo de falta para productos sin cambiar el contrato del reporte.

**Tablas:** Producto, Insumo (solo lectura). No se agregaron tablas ni columnas; no se modificó el módulo Stock ni Catálogo.

---

## REP-09. Entradas, salidas y descartes de stock ✅

**Qué construir:** el administrador ve en un período qué entró al stock (ingresos de mercadería), qué salió por ventas y qué salió por descarte, pérdida o ajuste.

**Bloqueado por:** 01: Ventas por período

**Respuesta de Stock recibida el 27/11/2026 y decisiones de Mayra confirmadas el 27/11/2026 (abajo).**

**Criterios de aceptación**
- [x] Tablas y tipos de movimiento: `Movimiento_Stock` (`ENTRADA`/`SALIDA`, con `motivo` `INICIAL`/`COMPRA`/`AJUSTE`/`MERMA`/`DEVOLUCION`).
- [x] Filtros: rango de fechas (obligatorio), artículo (producto o insumo), tipo de movimiento (`ENTRADA`/`SALIDA`) y proveedor.
- [x] Cada fila muestra artículo, unidad, entradas, salidas por venta, descartes, ajustes y la 5.ª columna "otras salidas".
- [x] Las cantidades se suman solo dentro de la misma unidad; los totales solo cuentan artículos y movimientos.
- [x] Gráfico de barras apiladas por artículo, con las 5 series de movimiento.
- [x] Exporta con lo de REP-02 (Excel, CSV y PDF, con los mismos filtros).
- [x] Solo administradores: el backend responde 403 al vendedor.
- [x] La pantalla avisa que las salidas por venta aparecerán cuando se implemente VEN-10.

**Respuesta de Stock (revisión solo lectura, 27/11/2026):** la tabla es `Movimiento_Stock` (`MovimientoStock.java:31-79`). Cada fila afecta **exactamente un** producto o un insumo (`id_producto` / `id_insumo`, uno de los dos), con `tipo` = `ENTRADA` o `SALIDA` (`TipoMovimientoStock.java`), `cantidad` positiva con 3 decimales, `motivo` ∈ `INICIAL`, `COMPRA`, `AJUSTE`, `MERMA`, `DEVOLUCION` (`MotivoMovimientoStock.java`), `id_usuario`, `fecha_hora` (`LocalDateTime`, sin zona), `observaciones`, `id_venta` y `id_ingreso_mercaderia` (ambos nullable). Los movimientos son **inmutables**: no tienen estado ni flag de anulación; las correcciones se registran como otro movimiento. `INICIAL` se genera automáticamente al dar de alta un producto o insumo con stock (`ProductoService.java:68-73`, `InsumoService.java:69-74`). `fecha_hora` se escribe con `LocalDateTime.now()` en la zona por defecto del servidor (`StockService.java:141`), no con el `Clock` del taller.

**Cómo se distingue una salida por venta de un descarte o ajuste:** no hay motivo `VENTA`. La única marca de venta es la FK `id_venta` (reservada para `VEN-10`, hoy 🔴 pendiente), que actualmente solo usan las devoluciones por anulación: `devolverStockDeVenta` busca `SALIDA` con `id_venta` y genera una `ENTRADA`/`DEVOLUCION` por cada una (`StockService.java:171-188`). Por lo tanto **hoy no se registra ninguna salida por venta**: la columna correspondiente daría 0 hasta que `VEN-10` implemente el descuento. `MERMA` es la pérdida/descarte y `AJUSTE` el ajuste manual, pero `StockService` **no valida combinaciones** de `tipo` y `motivo`: por la API se pueden crear tanto `ENTRADA/AJUSTE` como `SALIDA/COMPRA` o `SALIDA/DEVOLUCION` (`StockController.java:48-76`). El campo `id_ingreso_mercaderia` está reservado para `CST-06` y no hay tabla de ingreso de mercadería; las compras se cargan hoy como `ENTRADA/COMPRA` manuales.

**Proveedor:** `Movimiento_Stock` **no guarda proveedor** (ni columna ni FK). La relación existe en el artículo, no en el movimiento: `Proveedor_Insumo` asocia `Proveedor` con `Insumo` (`ProveedorInsumo.java:22-29`) y `ProveedorInsumoService` evita duplicar el par. Los **productos no tienen proveedor**. Un mismo insumo puede tener varios proveedores, y la relación refleja la asignación vigente, no qué proveedor entregó cada entrada.

**Decisiones aplicadas (Mayra, 27/11/2026):** (1) `entradas` = todas las `ENTRADA` (incluida `INICIAL`); `salidasVenta` = `SALIDA` con `id_venta` (tiene prioridad sobre el motivo; hoy da 0 hasta VEN‑10); `descartes` = `SALIDA`/`MERMA`; `ajustes` = `SALIDA`/`AJUSTE`; la 5.ª columna `otrasSalidas` = el resto de `SALIDA` (por ejemplo `COMPRA` o `DEVOLUCION`). (2) `INICIAL` cuenta como entrada. (3) El filtro "tipo de movimiento" es `ENTRADA`/`SALIDA`, no el motivo. (4) El artículo se elige en un selector combinado con productos e insumos rotulados; una sola fila por artículo y unidad `—` para productos. (5) El proveedor se resuelve por `Proveedor_Insumo` (solo insumos): al elegirlo se ocultan los productos con aviso, `EXISTS` evita duplicar insumos con varios proveedores y no hay columna de proveedor en la tabla. (6) El gráfico apila las 5 series dentro de cada artículo (una barra por artículo, nunca mezcla unidades) y los totales solo informan conteos. (7) El rango es obligatorio sobre `fecha_hora` como fechas de calendario (`>= desde 00:00` y `< hasta + 1 día`, sin conversión de zona); la exportación reutiliza REP‑02 con id `movimientos-stock`, archivo `movimientos-stock_<desde>_<hasta>.<ext>`, tabla sin gráfico y el aviso de VEN‑10 en el encabezado.

**Tablas:** `Movimiento_Stock`, `Producto`, `Insumo`, `Venta` (solo `id_venta`), `Proveedor` y `Proveedor_Insumo` (solo lectura). No se agregaron tablas ni columnas; no se modificaron los módulos Stock ni Ventas.

---

## REP-10. Costos por tipo y su evolución ✅

**Qué construir:** el administrador ve los costos de un período separados por tipo y cómo evolucionan mes a mes, incluida la evolución del costo de insumos.

**Bloqueado por:** 01: Ventas por período

**Respuesta recibida (revisión solo lectura de Costos, 27/11/2026):** el equipo ya respondió el cuestionario 04 en [04-costos-res.md](../costos/04-costos-res.md), y el módulo quedó implementado según el backlog de Costos. Un **costo** es todo gasto del taller con `descripcion`, `monto` (`numeric(12,2)`), `fecha_pago`, `periodo_mes`, `periodo_anio`, tipo y usuario que lo carga (`Costo.java:13-66`), con baja lógica (`anulado`, motivo, fecha y usuario de anulación). El período se guarda **en mes y año** además de la fecha de pago, lo elige quien carga y el servidor lo copia sin derivarlo ni validar que coincida con `fecha_pago` (`CostoCreateDTO.java:30-47`, `CostoService.java:51-77`). Los filtros actuales de `GET /api/costos` y `/api/costos/total` usan `periodo_mes`/`periodo_anio`, nunca `fecha_pago` (`CostoRepository.java:13-36`). Los tipos **no son un enum**: son una tabla administrable (`TipoCosto`, con `activo` y `reservado`); el único tipo sembrado es `Insumos`, reservado, que el sistema genera y no se puede cargar a mano ni editar (`TipoCostoInsumosInitializer.java:14-49`, `CostoService.java:100-111`, `TipoCostoService.java:58-65`). No hay tipos fijos `Sueldos`, `Servicios` ni `Otros`: si hacen falta, los crea un administrador. Los costos anulados no cuentan: el total siempre los excluye y el listado solo los incluye con `incluirAnulados=true`. La **evolución del costo de insumos** quedó decidida desde el costo unitario de cada ingreso de mercadería (`04-costos-res.md:27-28`), pero **hoy no está implementada**: `CST-06` sigue bloqueado, no hay tabla de ingreso de mercadería y solo quedan los campos reservados `id_ingreso_mercaderia` en `Costo` y `Movimiento_Stock`. `Insumo.costo` es un único valor que se sobrescribe en cada PUT (`InsumoService.java:98-100`); `Movimiento_Stock` guarda cantidades pero ningún precio ni monto; la auditoría registra cambios de `Insumo.costo` como JSON, pero su API no filtra por insumo ni agrupa por mes (`AuditoriaEntityListener.java:54-84`, `AuditoriaRepository.java:24-53`). Por lo tanto, hasta `CST-06` no hay una fuente completa para la serie de insumos; el resto de los tipos sí puede reportarse desde `Costo`.

**Decisiones de Mayra confirmadas el 27/11/2026 (las 7 recomendaciones):** (1) el rango es obligatorio y se expresa como `AAAA-MM` a `AAAA-MM`, inclusivo, sobre `periodo_anio` y `periodo_mes`; va de 2000 a 2100, con tope de 36 meses, y `fecha_pago` no interviene. (2) Los costos anulados se excluyen siempre. (3) El selector de tipos muestra los activos, más los inactivos que tengan costos en el rango y el reservado `Insumos`; con "Todos" la tabla muestra los tipos con costos en el rango y siempre `Insumos`, y con un tipo elegido muestra solo ese tipo (aunque dé 0). (4) La serie de `Insumos` se muestra en 0 con el aviso de que la evolución real espera a `CST-06`. (5) La tabla lleva meses en filas, tipos en columnas y una columna/fila `Total`. (6) El gráfico de líneas dibuja una línea por tipo sobre los mismos meses de la tabla. (7) La exportación reutiliza REP‑02 con id `costos-por-tipo`, nombre `costos-por-tipo_<desde>_<hasta>.<ext>` y tabla con totales, sin gráfico. Solo administradores se hereda de `/api/reportes/**`.

**Criterios de aceptación**
- [x] Rango de períodos `AAAA-MM` obligatorio, inclusivo, con tope de 36 meses y años 2000–2100; `fecha_pago` no interviene.
- [x] Filtros: rango de períodos (obligatorio) y tipo de costo.
- [x] Tabla con el total por tipo y por mes, completando con 0 los meses vacíos.
- [x] Gráfico de líneas, una por tipo de costo.
- [x] Los anulados quedan excluidos.
- [x] La evolución de insumos se muestra en 0 con aviso de `CST-06`.
- [x] Exporta con lo de REP-02 (Excel, CSV y PDF, con los mismos filtros).
- [x] Solo administradores (backend 403 para otros roles, no alcanza con ocultar el menú).

**Tablas:** `Costo` y `TipoCosto` (solo lectura). Sin tablas nuevas; no se modificó el módulo Costos.

---

## REP-11. Resultado del período (ventas menos costos) ✅

**Qué construir:** el administrador ve, mes a mes, el total vendido, el total de costos y la diferencia entre los dos.

**Bloqueado por:** 01: Ventas por período; 10: Costos por tipo y su evolución

**Respuesta de Costos recibida:** la pregunta 4 (período) quedó respondida en [04-costos-res.md](../costos/04-costos-res.md) e implementada con `periodo_mes` y `periodo_anio`; REP‑10 ya está listo con el rango en meses y sin `fecha_pago`, y REP‑11 reutiliza esa misma consulta.

**Decisiones de Mayra confirmadas el 27/11/2026 (las 7 recomendaciones):** (1) el rango `AAAA-MM` a `AAAA-MM` es obligatorio e inclusivo, con tope de 36 meses y años 2000–2100; las ventas cuentan por su fecha de venta y los costos por `periodo_anio`/`periodo_mes`, sin `fecha_pago`. (2) No hay filtro de tipo de costo: entran todos, sin distinción. (3) La tabla incluye todos los meses del rango, con 0 en los vacíos. (4) El resultado negativo se muestra con signo y en rojo, sin recortarlo a 0. (5) Las columnas son `Período | Total vendido | Total costos | Resultado`, con una fila `Totales`; el gráfico combinado dibuja dos barras (vendido y costos) y una línea (resultado) sobre el mismo eje y meses. (6) La pantalla aclara que es un resultado simple, no un balance contable (no considera stock sin vender ni amortizaciones), y repite el aviso de que la evolución real de `Insumos` espera a `CST-06`. (7) La exportación reutiliza REP‑02 con id `resultado-periodo`, nombre `resultado-periodo_<desde>_<hasta>.<ext>` y la tabla sin gráfico. Solo administradores se hereda de `/api/reportes/**`.

**Criterios de aceptación**
- [x] Por cada mes: total vendido (ventas confirmadas), total de costos y resultado = vendido − costos.
- [x] Rango `AAAA-MM` obligatorio e inclusivo, tope de 36 meses y años 2000–2100; ventas por fecha de venta y costos por `periodo_anio`/`periodo_mes`, sin `fecha_pago`.
- [x] Sin filtro de tipo de costo; meses vacíos en 0.
- [x] Resultado negativo con signo y en rojo, sin recortar.
- [x] Gráfico combinado: barras de vendido y costos, línea de resultado, mismo eje y meses.
- [x] Aviso de resultado simple, no balance contable, y aviso de `Insumos`/`CST-06`.
- [x] Usa los servicios de REP-01 y REP-10 para que los números coincidan con esos reportes.
- [x] Exporta con REP-02 (Excel, CSV y PDF) con los mismos filtros; la tabla no incluye el gráfico.
- [x] Solo administradores (backend 401 sin sesión y 403 para otros roles).

**Tablas:** `Venta` y `Costo` (solo lectura, a través de los servicios de REP‑01 y REP‑10). Sin tablas nuevas; no se modificaron los módulos Ventas, Costos ni Stock.

---

## REP-12. Producción: productos elaborados y consumo de insumos 🔒

**Qué construir:** el administrador ve cuánto se elaboró de cada producto en un período y cuántos insumos se consumieron.

**Bloqueado por:** 01: Ventas por período

**Espera respuesta a:** Stock 1: ¿hay registro de producción? **Respondido por revisión solo lectura el 27/11/2026: no existe.** Quedan abiertas las decisiones de modelo y carga que se listan abajo. No se implementó nada de REP‑12.

**Revisión 27/11/2026 (solo lectura):** no hay entidad, tabla, DTO, endpoint, pantalla ni test de producción. En Catálogo existe `Producto_Insumo` (`ProductoInsumo.java:9-34`) como **receta estática**: producto, insumo, `cant_necesaria` y `unidad_medida`; no guarda fecha, usuario, cantidad elaborada, consumo real ni lote (`docs/modelo-datos.md:50-60`). En Stock, `Movimiento_Stock` (`MovimientoStock.java:42-78`) mueve **un solo** producto o insumo por vez con `tipo` `ENTRADA`/`SALIDA` y motivos `INICIAL`, `COMPRA`, `AJUSTE`, `MERMA`, `DEVOLUCION` (`MotivoMovimientoStock.java:8-13`); no hay motivo `PRODUCCION`, FK de producción ni vínculo que agrupe la entrada del producto con las salidas de sus insumos. `StockService.java:93-213` registra un movimiento por vez y actualiza un saldo; no tiene una carga transaccional que descuente insumos e ingrese el producto. Tampoco hay migraciones SQL/Flyway (`application.properties:6` usa `ddl-auto=update`). `Detalle_Venta` guarda lo vendido, `Costo` guarda montos y períodos, y Caja, Incidencias y Auditoría no registran elaboración. REP‑09 no puede reemplazar este registro: una `ENTRADA` de producto y una `SALIDA` de insumo aparecen en artículos distintos, las salidas sin venta/MERMA/AJUSTE caen en “otras salidas” y `observaciones` es texto libre.

**Qué haría falta agregar y dónde:** el hecho operativo pertenece a **Stock**: cabecera de producción (id, fecha/hora, usuario, estado/anulación, observaciones), línea del producto elaborado (producto y cantidad), líneas de insumos consumidos (insumo, cantidad efectiva y unidad) y vinculación con los `Movimiento_Stock` que generan la entrada y las salidas, con servicio transaccional, DTOs, endpoint y pantalla de carga. **Catálogo** aportaría la receta `Producto_Insumo` como plantilla y, si se decide, la versión vigente al producir; `Insumo.costo` no sirve como costo histórico de consumo. **Reportes** recién después agregaría REP‑12 como solo lectura (consulta + DTO + pantalla + REP‑02), sin tablas propias. Como no hay migraciones, también habría que definir la fuente del esquema (export de Vertabelo / eventual Flyway, `DB-01`).

**Decisiones abiertas (para Mayra/el equipo):** (1) ¿producción es un asiento ya ejecutado o una orden con estados (borrador/confirmada/anulada)? (2) ¿siempre contra una receta `Producto_Insumo` o se permite consumo libre, guardando o no la versión usada? (3) ¿una producción genera siempre un producto o varios? (4) ¿se guardan lote/partida y unidad de venta del producto (`Producto` hoy no tiene `unidad_venta`)? (5) ¿las correcciones son un nuevo asiento o una anulación? (6) ¿quién puede cargarla: cualquier autenticado, como los ajustes de Stock, o solo administradores? (7) ¿se vincula económicamente con `Costo`/`CST‑06` o queda solo en cantidades? (8) ¿cómo se clasifican las salidas de insumos: nuevo motivo de stock o FK de producción?

**Criterios de aceptación**
- [ ] Confirmado por revisión: hoy no existe el registro de producción; REP‑12 queda bloqueado hasta que Stock lo implemente.
- [ ] Filtros: rango de fechas (obligatorio), producto e insumo.
- [ ] Tabla de productos elaborados (cantidad por producto) y tabla de insumos consumidos (cantidad por insumo y unidad).
- [ ] Gráfico de barras por producto.
- [ ] Exporta con lo de REP-02.

**Tablas:** (a definir por Stock; hoy no existe ninguna tabla de producción)

---

## REP-13. Incidencias y descartes 🔒

**Qué construir:** el administrador ve las incidencias de un período por tipo, producto y estado, y los descartes, para encontrar problemas que se repiten.

**Bloqueado por:** 01: Ventas por período

**Espera respuesta a:** Cuestionario 09 de Incidencias

**Criterios de aceptación**
- [ ] (A confirmar con el módulo Incidencias) Tipos y estados de incidencia.
- [ ] Filtros: rango de fechas (obligatorio), producto, tipo y estado.
- [ ] Tabla con cantidad de incidencias y cantidad afectada por tipo y por producto.
- [ ] Incluye los descartes de stock del período.
- [ ] Gráfico de barras por tipo de incidencia.
- [ ] Exporta con lo de REP-02.

**Tablas:** Incidencia, salidas por descarte (a definir)

---

## REP-14. Facturación 🔒

**Qué construir:** el administrador ve el total facturado en un período. Depende de que el sistema emita facturas, lo que todavía no está definido en Ventas.

**Bloqueado por:** 01: Ventas por período

**Espera respuesta a:** Facturación: ¿factura electrónica de ARCA o comprobante interno? Lo decide Ventas (pregunta 15)

**Criterios de aceptación**
- [ ] (A definir cuando Ventas decida la facturación) Qué es una factura en el sistema y cómo se anula.
- [ ] No se arma con el total vendido: ese ya está en REP-01.
- [ ] Filtros, tabla y gráfico a definir.
- [ ] Exporta con lo de REP-02.

**Tablas:** (a definir por Ventas)

---

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
| REP-07 | Reporte de cierres de caja | REP-01, CAJA-05 | Listo para empezar |
| REP-08 | Stock actual y productos bajo mínimo | REP-01 | 🔒 Falta respuesta |
| REP-09 | Entradas, salidas y descartes de stock | REP-01 | 🔒 Falta respuesta |
| REP-10 | Costos por tipo y su evolución | REP-01 | 🔒 Falta respuesta |
| REP-11 | Resultado del período (ventas menos costos) | REP-01, REP-10 | 🔒 Falta respuesta |
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
- [ ] Filtros: rango de fechas (obligatorio), usuario que cerró, estado de la sesión y medio de pago.
- [ ] Cada fila muestra la sesión: fecha y hora de apertura y cierre, quién abrió y quién cerró, monto inicial, esperado, declarado y diferencia por medio de pago, y estado.
- [ ] Totales del período por medio de pago: ingresos, egresos, faltantes y sobrantes.
- [ ] Resalta las sesiones en pendiente de revisión.
- [ ] Si un cierre tuvo una corrección, muestra el valor vigente y marca que fue corregido.
- [ ] Gráfico de barras de la diferencia de cada sesión (negativas y positivas).
- [ ] Exporta con lo de REP-02.

**Tablas:** Sesion_Caja, Cierre_Caja_Detalle, Movimiento_Caja (solo lectura)

---

## REP-08. Stock actual y productos bajo mínimo 🔒

**Qué construir:** el administrador ve el stock actual de productos e insumos y, en especial, los que están bajo mínimo.

**Bloqueado por:** 01: Ventas por período

**Espera respuesta a:** Cuestionario 06 de Stock (cómo se guardan stock actual y mínimo)

**Criterios de aceptación**
- [ ] (A confirmar con el módulo Stock) De dónde sale el stock actual: columna en Producto/Insumo o suma de movimientos.
- [ ] Filtros: tipo (producto o insumo), categoría y "solo bajo mínimo".
- [ ] Cada fila muestra artículo, unidad, stock actual, stock mínimo y cuánto falta para llegar al mínimo.
- [ ] Gráfico de barras stock actual contra mínimo para los bajo mínimo.
- [ ] No lleva rango de fechas: es una foto de hoy.
- [ ] Exporta con lo de REP-02.

**Tablas:** (a definir por Stock)

---

## REP-09. Entradas, salidas y descartes de stock 🔒

**Qué construir:** el administrador ve en un período qué entró al stock (ingresos de mercadería), qué salió por ventas y qué salió por descarte, pérdida o ajuste.

**Bloqueado por:** 01: Ventas por período

**Espera respuesta a:** Cuestionario 06 de Stock

**Criterios de aceptación**
- [ ] (A confirmar con el módulo Stock) Tablas y tipos de movimiento.
- [ ] Filtros: rango de fechas (obligatorio), artículo, tipo de movimiento y proveedor.
- [ ] Cada fila muestra artículo, unidad y cantidades de entradas, salidas por venta, descartes y ajustes.
- [ ] Las cantidades se suman solo dentro de la misma unidad.
- [ ] Gráfico de barras apiladas por tipo de movimiento.
- [ ] Exporta con lo de REP-02.

**Tablas:** (a definir por Stock)

---

## REP-10. Costos por tipo y su evolución 🔒

**Qué construir:** el administrador ve los costos de un período separados por tipo (insumos, sueldos, servicios, otros) y cómo evolucionan mes a mes, incluido el costo de los insumos.

**Bloqueado por:** 01: Ventas por período

**Espera respuesta a:** Cuestionario 04 de Costos

**Criterios de aceptación**
- [ ] (A confirmar con Costos 1, 4, 5 y 7) Qué es un costo, cómo se asigna a un período, qué tipos hay y de dónde sale la evolución del costo de insumos.
- [ ] Filtros: rango de períodos (obligatorio) y tipo de costo.
- [ ] Tabla con el total por tipo y por mes.
- [ ] Gráfico de líneas, una por tipo de costo.
- [ ] Exporta con lo de REP-02.

**Tablas:** Costo (a definir por Costos)

---

## REP-11. Resultado del período (ventas menos costos) 🔒

**Qué construir:** el administrador ve, mes a mes, el total vendido, el total de costos y la diferencia entre los dos.

**Bloqueado por:** 01: Ventas por período; 10: Costos por tipo y su evolución

**Espera respuesta a:** Costos 4: cómo se asigna un costo a un período

**Criterios de aceptación**
- [ ] Por cada mes: total vendido (ventas confirmadas), total de costos y resultado = vendido − costos.
- [ ] Gráfico de barras vendido contra costos, con una línea del resultado.
- [ ] La pantalla aclara que es un resultado simple, no un balance contable.
- [ ] Usa las mismas consultas que REP-01 y REP-10 para que los números coincidan con esos reportes.
- [ ] Exporta con lo de REP-02.

**Tablas:** Venta, Costo (solo lectura)

---

## REP-12. Producción: productos elaborados y consumo de insumos 🔒

**Qué construir:** el administrador ve cuánto se elaboró de cada producto en un período y cuántos insumos se consumieron.

**Bloqueado por:** 01: Ventas por período

**Espera respuesta a:** Stock 1: ¿hay registro de producción?

**Criterios de aceptación**
- [ ] (A confirmar con Stock 1) Que exista un registro de producción; hoy el diagrama no tiene cómo registrar lo que se elabora.
- [ ] Filtros: rango de fechas (obligatorio), producto e insumo.
- [ ] Tabla de productos elaborados (cantidad por producto) y tabla de insumos consumidos (cantidad por insumo y unidad).
- [ ] Gráfico de barras por producto.
- [ ] Exporta con lo de REP-02.

**Tablas:** (a definir por Stock)

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

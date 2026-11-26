# Tickets: módulo Reportes

Fuente: [reportes-decisiones.md](../reportes-decisiones.md), [GLOSSARY.md](../GLOSSARY.md) y el ADR 0004. Reportes no tiene tablas propias: cada ticket es una consulta de solo lectura + su pantalla (filtros, tabla y gráfico) + su exportación.

Cada ticket es una **rebanada vertical** (consulta, backend y pantalla) que se puede probar de punta a punta. "Bloqueado por" dice qué tiene que estar terminado antes; los marcados 🔒 además esperan que se responda otro módulo y no se empiezan hasta tenerlo.

**Requisitos previos:** login con rol Administrador, y ventas cargadas (VEN-01) para probar.

| # | Ticket | Bloqueado por | Estado |
| --- | --- | --- | --- |
| REP-01 | Ventas por período | VEN-01 | Listo para empezar |
| REP-02 | Exportar un reporte a Excel, CSV y PDF | REP-01 | Listo para empezar |
| REP-03 | Ventas por producto y productos más vendidos | REP-01 | Listo para empezar |
| REP-04 | Ventas por cliente y por tipo de cliente | REP-01, CLI-01 | Listo para empezar |
| REP-05 | Ventas por vendedor | REP-01 | Listo para empezar |
| REP-06 | Evolución de ventas y comparación estacional | REP-01 | Listo para empezar |
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
- [ ] Solo los administradores entran a Reportes: el backend responde 403 a cualquier otro rol, no alcanza con ocultar el menú.
- [ ] El rango de fechas es obligatorio y las dos fechas se incluyen completas (el 31 cuenta entero).
- [ ] Se puede agrupar por día, semana o mes.
- [ ] Cada fila muestra el período, el total vendido, la cantidad de ventas y el ticket promedio; abajo, el total del rango.
- [ ] Solo cuentan las ventas confirmadas; las anuladas no, aunque se hayan anulado después.
- [ ] Los períodos sin ventas aparecen en 0 (se completan en Java).
- [ ] Gráfico de barras del total vendido por período.
- [ ] La consulta devuelve un DTO (no entidades) y usa SQL que funciona igual en Postgres 15 y 17.
- [ ] Test: ventas confirmadas y anuladas en distintos días; el total coincide con la suma de Venta.total de las confirmadas.

**Tablas:** Venta (solo lectura)

---

## REP-02. Exportar un reporte a Excel, CSV y PDF

**Qué construir:** desde cualquier reporte, el administrador descarga exactamente lo que está viendo (con los filtros aplicados) en Excel, CSV o PDF. Se prueba con Ventas por período y queda reutilizable para el resto.

**Bloqueado por:** 01: Ventas por período

**Criterios de aceptación**
- [ ] Botones Excel, CSV y PDF en la pantalla del reporte.
- [ ] El archivo se genera en el backend con la misma consulta y los mismos filtros que la pantalla.
- [ ] Excel con encabezados, montos como números (no texto) y una fila de total.
- [ ] CSV en UTF-8, con separador punto y coma, para que Excel en español lo abra bien.
- [ ] PDF con título del reporte, rango de fechas, fecha de emisión y la tabla.
- [ ] El nombre del archivo lleva el reporte y el rango, por ejemplo ventas-por-periodo_2026-10-01_2026-10-31.xlsx.
- [ ] La exportación es una pieza común: agregarla a otro reporte no requiere escribirla de nuevo.
- [ ] Solo administradores.

**Tablas:** Ninguna (usa el DTO del reporte)

---

## REP-03. Ventas por producto y productos más vendidos

**Qué construir:** el administrador ve, para un período, cuánto se vendió de cada producto (en pesos y en cantidad) y el ranking de los más vendidos. Cubre del PDF: ventas por producto, cantidades vendidas, ventas por unidad, ventas por peso y productos más vendidos.

**Bloqueado por:** 01: Ventas por período

**Criterios de aceptación**
- [ ] Filtros: rango de fechas (obligatorio), producto y categoría.
- [ ] Cada fila muestra producto, categoría, unidad de venta, cantidad vendida y total vendido.
- [ ] El total vendido sale de la suma de Detalle_Venta.subtotal de las ventas confirmadas, no del precio actual del catálogo.
- [ ] Las cantidades se suman solo dentro de la misma unidad: nunca se suman unidades con kilos.
- [ ] Se puede ver el total separado entre productos que se venden por unidad y por peso.
- [ ] Ranking de los N más vendidos por total vendido (N elegible, por defecto 10), con gráfico de barras horizontales.
- [ ] Exporta con lo de REP-02.

**Tablas:** Detalle_Venta, Venta, Producto, Categoria (solo lectura)

---

## REP-04. Ventas por cliente y por tipo de cliente

**Qué construir:** el administrador ve cuánto le compró cada cliente en un período, y el total por tipo de cliente (gran cliente y consumidor final).

**Bloqueado por:** 01: Ventas por período; CLI-01: Registrar un cliente

**Criterios de aceptación**
- [ ] Filtros: rango de fechas (obligatorio), cliente y tipo de cliente.
- [ ] El filtro de cliente permite elegir también clientes inactivos.
- [ ] Cada fila muestra cliente, tipo, cantidad de compras y total vendido, ordenado de mayor a menor.
- [ ] Las ventas de mostrador aparecen en el cliente genérico "Consumidor final" como un cliente más.
- [ ] Una vista resumida muestra el total por tipo de cliente, con gráfico de barras.
- [ ] Exporta con lo de REP-02.

**Tablas:** Venta, Cliente (solo lectura)

---

## REP-05. Ventas por vendedor

**Qué construir:** el administrador ve cuánto vendió cada vendedor en un período. Vendedor es el usuario que registró la venta.

**Bloqueado por:** 01: Ventas por período

**Criterios de aceptación**
- [ ] Filtros: rango de fechas (obligatorio) y vendedor.
- [ ] Cada fila muestra vendedor, cantidad de ventas, total vendido y ticket promedio, con gráfico de barras.
- [ ] Incluye a los administradores si registraron ventas.
- [ ] Si Ventas 10 se responde "sí" (vendedor distinto de quien carga), la consulta pasa a usar esa columna.
- [ ] Exporta con lo de REP-02.

**Tablas:** Venta, Usuario (solo lectura)

---

## REP-06. Evolución de ventas y comparación estacional

**Qué construir:** el administrador ve cómo evolucionan las ventas mes a mes y compara cada mes con el mismo mes de años anteriores, con una línea por año en el mismo gráfico.

**Bloqueado por:** 01: Ventas por período

**Criterios de aceptación**
- [ ] Filtros: años a comparar (por defecto el actual y los dos anteriores), producto y categoría.
- [ ] Gráfico de líneas superpuestas: eje X de enero a diciembre, una línea por año.
- [ ] Tabla con meses en filas y años en columnas, más la variación porcentual contra el año anterior.
- [ ] Los meses sin ventas valen 0; los meses que todavía no pasaron no se dibujan.
- [ ] Agrupa con EXTRACT(YEAR) y EXTRACT(MONTH) sobre la fecha de la venta, solo confirmadas.
- [ ] Exporta con lo de REP-02.

**Tablas:** Venta, Detalle_Venta (solo lectura)

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

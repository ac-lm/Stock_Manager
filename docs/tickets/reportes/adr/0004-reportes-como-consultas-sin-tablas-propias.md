# 0004. Los reportes son consultas de solo lectura, sin tablas propias

- Estado: aceptada
- Fecha: 2026-11-26

## Contexto

El PDF pide reportes de ventas, stock, costos, caja y producción, con tablas, gráficos, filtros y exportación. La base tiene que funcionar igual en Postgres 15 y 17 y el servidor de la clienta es chico. Había que decidir si los reportes calculan todo al momento o si se guardan totales ya calculados.

## Decisión

Cada reporte es una **consulta de solo lectura** sobre las tablas de los otros módulos, que devuelve un DTO. Reportes no tiene tablas propias. Si una consulta se repite mucho o se vuelve lenta, se guarda como **vista SQL común** (no materializada). La exportación usa la misma consulta que la pantalla.

## Alternativas consideradas

- **Tablas de resumen** (por ejemplo, ventas por día) que se actualizan al vender. Más rápidas con muchos datos, pero hay que mantenerlas sincronizadas en cada venta, anulación y corrección; un error deja reportes que no coinciden con las ventas.
- **Vistas materializadas.** Existen en Postgres 15 y 17, pero hay que refrescarlas y entre refresco y refresco muestran datos viejos.

## Consecuencias

- Los reportes siempre coinciden con los datos de hoy (por ejemplo, una venta anulada desaparece del reporte de su mes).
- Cada reporte depende de que exista el módulo del que lee.
- Hacen falta índices en las columnas de fecha y en las FK que se agrupan. Si con años de datos algún reporte se pone lento, se revisa esta decisión para ese reporte puntual.

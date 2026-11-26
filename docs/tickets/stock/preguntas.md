# Módulo Stock: decisiones y preguntas

Registro de las decisiones tomadas para la primera tanda, los puntos resueltos y el único
pendiente (el export de Vertabelo).
Si cambia una decisión de negocio, se actualiza acá primero y después el
[`backlog.md`](backlog.md).

## Decisiones vigentes

| # | Tema | Decisión | Impacto |
|---|---|---|---|
| 1 | Alcance | Opción B de alcance: núcleo de movimientos + ajuste manual + kardex + pantalla; después se integró CAJA-07 | `VEN-10`, `INC-07` y `CST-06` siguen pendientes |
| 2 | Stock negativo | **No se permite.** Toda salida que supere el saldo → `409` y rollback | Se revirtió la decisión 8b de `ventas/preguntas.md`; `VEN-10` quedó actualizado |
| 3 | Pantallas | `/stock/insumos` y `/stock/productos`, separadas, visibles para ambos roles | Se agregan dos links en la navbar, sin `soloAdmin` |
| 4 | Usuario | Se toma de `UsuarioService.obtenerAutenticado()`, nunca del body | Igual que caja y costos |
| 5 | Movimientos | Inmutables: no se editan ni se borran; las correcciones son nuevos movimientos | Igual que `Movimiento_Caja` |
| 6 | Saldo inicial | **Opción A confirmada el 26/11/2026:** el alta mantiene el campo y genera un movimiento `INICIAL` a través de `StockService` | El formulario de alta no cambia; queda trazabilidad |
| 7 | Modelo | La propuesta de columnas de `STOCK-01` se usa mientras no se confirme el XML de Vertabelo | Hay que validarla antes de generar la entidad |
| 8 | Fecha del movimiento | Un único `fecha_hora timestamp`, sin `fecha`+`hora` separadas | Define la columna, los DTOs y los filtros del kardex |
| 9 | Devolución al anular (CAJA-07, 26/11/2026) | Se revierten solo las `SALIDA` que tengan `id_venta`; si no hay, no se crea stock | Evita generar mercadería de la nada mientras `VEN-10` no esté implementado |

## Puntos resueltos y pendientes

1. **Stock negativo vs. `VEN-10` (resuelto el 26/11/2026).** Se revirtió la decisión 8b de
   ventas: `VEN-10`, `ventas/preguntas.md` y `ventas/backlog.md` ya reflejan "stock nunca
   negativo" con `409`. No queda conflicto pendiente.

2. **Producto e insumo en el mismo movimiento (confirmado).** Un movimiento afecta
   **exactamente uno**. Si una incidencia afecta a ambos, al integrar `INC-07` se generará un
   movimiento por cada ítem.

3. **Alta de catálogo con stock (resuelto el 26/11/2026).** Se confirma la **opción A**: se
   mantiene el campo de stock inicial y `ProductoService`/`InsumoService` registran un movimiento
   `INICIAL` vía `StockService` con inyección `@Lazy`. La opción B (alta con `0` y ajuste posterior)
   quedó descartada.

4. **Quién puede registrar ajustes (confirmado).** Cualquier usuario autenticado; las pantallas
   son visibles para ambos roles y no se agrega restricción de `ADMINISTRADOR`.

5. **Fecha del movimiento (resuelto).** Se usa un único `fecha_hora timestamp`. Queda
   registrado en la decisión 8 y en `STOCK-01`.

6. **XML de Vertabelo (único pendiente).** `docs/db/README.md` lo declara fuente de verdad pero
   todavía no está en `docs/db/modelo-vertabelo.xml`. `docs/modelo-datos.md` lista
   `Movimiento_Stock` como tabla pendiente, sin columnas. Si aparece el export/PDF se copian los
   nombres exactos; si no, se usa la propuesta de `STOCK-01`.

7. **Anulación de ventas (resuelto el 26/11/2026).** CAJA-07 devuelve stock solo cuando existen
   `SALIDA` con `id_venta`. Hoy no hay descuento al confirmar (`VEN-10` sigue pendiente), así que
   anular no crea stock. Cuando se implemente `VEN-10`, sus salidas deben llevar la FK para que
   la devolución se haga sola.

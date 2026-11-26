# Gestión de tickets

Backlog del proyecto organizado por módulo. La idea es que cada ticket declare **qué lo bloquea**
y **qué desbloquea**, para poder avanzar de a poco sin arrancar algo que todavía no tiene sus
dependencias resueltas.

## Convenciones

- **ID:** `<MODULO>-<NN>`, por ejemplo `INC-04`, `USU-01`, `STOCK-01`.
- **Estados:** ✅ Hecho · 🟡 En curso · 🟢 Listo · 🔴 Bloqueado.
- Un ticket pasa a **🟢 Listo** solo cuando todos sus bloqueantes están ✅.
- Un ticket **🔴 Bloqueado** siempre indica el ID de lo que lo bloquea.
- **Etiquetas:** nombre del módulo (`incidencias`, `stock`, …) + tipo (`backend`, `frontend`, `db`, `tests`, `docs`, `diseño`).
- No se cierra un ticket sin cumplir sus criterios de aceptación y sin correr los comandos de verificación.
- Los tickets de varios módulos se agrupan en `docs/tickets/<modulo>/`.
- Cada módulo tiene un `backlog.md` como fuente de verdad del alcance y las dependencias.

## Mapa de dependencias entre módulos

| Módulo | Estado | Depende de | Habilita |
|---|---|---|---|
| `common` | ✅ | — | Todos (errores y utilidades) |
| `catalogo` | ✅ | — | `incidencias`, `stock`, `ventas`, `pedidos`, `costos`, `reportes` |
| `clientes` | ✅ | — | `ventas`, `pedidos`, `reportes` |
| `proveedores` | ✅ | `catalogo` (insumos) | `costos`, `reportes` |
| `usuario` | 🟡 | — | `incidencias`, `ventas`, `stock`, `auditoria` |
| `stock` | 🔴 | `catalogo`, `usuario` | `incidencias`, `ventas`, `pedidos`, `costos`, `reportes` |
| `incidencias` | 🔴 | `catalogo` ✅, `usuario`, `stock` (solo descuento) | `reportes` |
| `ventas` | 🔴 | `catalogo`, `clientes`, `usuario`, `stock` | `caja`, `reportes`, `auditoria` |
| `pedidos` | 🔴 | `clientes`, `catalogo`, `stock` | `reportes` |
| `caja` | 🔴 | `ventas`, `usuario` | `reportes` |
| `costos` | 🟡 | `catalogo`, `proveedores`, `stock`, `caja` (integraciones) | `reportes` |
| `auditoria` | 🔴 | `usuario` (+ eventos de los demás módulos) | — (transversal) |
| `reportes` | 🔴 | Todos | — |

### Lectura rápida

- **`stock` es el desbloqueante más importante.** Sin movimientos de stock no se pueden cerrar
  incidencias con descuento, confirmar ventas, recibir pedidos ni calcular costos.
- **`usuario` es el segundo.** Varios módulos necesitan validar y registrar quién hizo cada cosa.
- **`catalogo`, `clientes`, `proveedores` y `common` ya están**, así que no bloquean a nadie.
- **`stock` tiene el backlog definido y revisado** ([`stock/backlog.md`](stock/backlog.md));
  queda listo para implementar cuando se dé la señal.
- **`costos` tiene el backend y la pantalla completos** (modelo, tipos, alta, edición, anulación,
  filtros, totales, tests, Swagger y frontend); la integración con caja (CST-05) ya está hecha y
  le faltan la integración con ingresos de mercadería (CST-06) y la auditoría de monto (CST-08).
- **`reportes` va último**: depende de que existan datos de todos los módulos.
- **`auditoria` es transversal**: conviene dejarla para cuando los módulos principales ya generen eventos.

## Backlogs

| Módulo | Backlog |
|---|---|
| `clientes` | [`clientes/backlog.md`](clientes/backlog.md) |
| `costos` | [`costos/backlog.md`](costos/backlog.md) |
| `incidencias` | [`incidencias/backlog.md`](incidencias/backlog.md) |
| `incidencias` (preguntas) | [`incidencias/preguntas.md`](incidencias/preguntas.md) |
| `stock` | [`stock/backlog.md`](stock/backlog.md) |
| `stock` (preguntas) | [`stock/preguntas.md`](stock/preguntas.md) |
| `ventas` | [`ventas/backlog.md`](ventas/backlog.md) |
| `ventas` (preguntas) | [`ventas/preguntas.md`](ventas/preguntas.md) |
| `usuarios` | [`usuarios/backlog.md`](usuarios/backlog.md) |

## Seguimiento en Trello

El backlog se importó a un tablero de Trello para el seguimiento diario. Esa importación se hizo
localmente y **los archivos auxiliares no se versionan** en el repositorio.

La fuente de verdad del alcance, el estado y las dependencias sigue siendo el `backlog.md` de
cada módulo. Trello es solo una vista de trabajo.

> Trello no tiene importación nativa de CSV; se usó un Power-Up de terceros.

## Cómo mantenerlo

- Al terminar un ticket, actualizar su estado en `backlog.md` y mover la tarjeta en Trello.
- Al agregar un ticket nuevo, asignarle ID, bloqueantes y criterios de aceptación.
- Si cambia una decisión de negocio, actualizar primero el `preguntas.md` del módulo y después
  los tickets afectados.

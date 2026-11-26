# Módulo Auditoría: decisiones e implementación

Carpeta nueva para no modificar documentación de otros módulos. Responde las preguntas 7 a 10
de [`../tickets/usuarios/10-usuarios-auditoria.md`](../tickets/usuarios/10-usuarios-auditoria.md).

## Qué se audita

- **Automáticamente:** altas, modificaciones y bajas (`ALTA`, `MODIFICACION`, `BAJA`) de
  cualquier entidad JPA, excepto la propia tabla `Auditoria`. Lo hace
  `AuditoriaEntityListener`, registrado como `Integrator` de Hibernate desde
  `auditoria/config/AuditoriaHibernateConfig`. Ninguna entidad ajena fue modificada.
- **Login correcto:** `LoginAuditoriaInterceptor`, registrado solo para
  `POST /api/auth/login`, escribe `LOGIN` después de una respuesta 200. No se auditan los
  login fallidos ni las consultas.
- **Acciones de negocio:** `POST /api/auditoria` permite registrar `APERTURA_CAJA`,
  `CIERRE_CAJA`, `CORRECCION_CAJA`, `ANULACION_VENTA`, `AJUSTE_STOCK`, `OTRO` y
  `CORRECCION_AUDITORIA`. La fecha y el usuario se toman de la sesión; el cliente no puede
  inventar el autor. `LOGIN` queda reservado al interceptor.

> Para que el cierre de caja, la anulación de venta y el ajuste de stock se registren **solos**
> hace falta agregar una llamada explícita en `CajaService`, `VentaService` y el futuro
> `StockService`. Eso implica tocar esos módulos y se dejó fuera de este cambio a propósito.

## Cómo se guarda el cambio

`Auditoria.cambios` es `jsonb` con la forma:

```json
{
  "stock_actual": { "anterior": 10, "nuevo": 15 },
  "nombre": { "anterior": "Viejo", "nuevo": "Nuevo" }
}
```

- Se usan **nombres de columna** (`stock_actual`), no de atributo Java (`stockActual`).
- Solo se incluyen campos básicos que cambiaron; se omiten asociaciones, colecciones y
  campos sensibles (`password`, `token`, `clave`, `hash`, etc.), incluso dentro del JSON
  que envía el cliente.
- En `ALTA` el valor anterior es `null`; en `BAJA` el nuevo es `null`.
- La fila se escribe en la **misma transacción** que la operación: si la operación se revierte,
  también se revierte la auditoría. Si falla la escritura de auditoría se registra un warning,
  nunca se impide la operación de negocio.

## Correcciones de cargas manuales

No hay `PUT` ni `DELETE`: las filas son evidencia. Si una carga manual estuvo mal, se agrega
una fila nueva con `accion = CORRECCION_AUDITORIA` y `id_auditoria_corregida` apuntando a la
original, que queda intacta. El `detalle` se compone automáticamente como
`Corrige registro #N: <motivo>` y `cambios` usa la misma forma de siempre.

Para el frontend, `origen` se deduce de la acción (`ALTA`, `MODIFICACION`, `BAJA` y `LOGIN`
son automáticos; el resto son manuales). Así se puede mostrar el botón **Corregir** sin
agregar una columna nueva que obligaría a backfill en bases ya existentes.

## Estructura de la tabla `Auditoria`

| Columna | Tipo | Restricciones |
|---|---|---|
| `id_auditoria` | `bigint` | PK, autoincremental |
| `fecha_hora` | `timestamp` | not null; reemplaza `fecha` + `hora` del diagrama |
| `id_usuario` | `bigint` | nullable; sin FK para no depender de otro módulo |
| `usuario` | `varchar(255)` | nullable; email congelado o `SISTEMA` |
| `accion` | `varchar(30)` | not null; enum `AccionAuditoria` |
| `tabla` | `varchar(50)` | nullable |
| `id_registro` | `bigint` | nullable |
| `id_auditoria_corregida` | `bigint` | nullable; solo en `CORRECCION_AUDITORIA`, apunta a la fila enmendada |
| `cambios` | `jsonb` | nullable; json de valores anterior/nuevo |
| `detalle` | `varchar(255)` | nullable |

No hay borrado ni actualización de filas: son evidencia. El esquema se crea con
`spring.jpa.hibernate.ddl-auto=update`, igual que el resto del proyecto (no hay migraciones
Flyway todavía).

## API

Base: `/api/auditoria`. Todo requiere sesión de **administrador**; la verificación está en
`AuditoriaService`, sin tocar `common/security`. Un anónimo recibe 401 y un vendedor 403.

| Método | Ruta | Uso |
|---|---|---|
| GET | `/api/auditoria?id_usuario=&accion=&tabla=&desde=&hasta=&page=&size=` | Listado paginado, más reciente primero |
| GET | `/api/auditoria/{id_auditoria}` | Detalle de un registro |
| POST | `/api/auditoria` | Alta manual de una acción de negocio o corrección de una carga manual |

Filtros: `accion` acepta los valores del enum, `tabla` es búsqueda parcial case-insensitive,
`desde`/`hasta` son `LocalDateTime` ISO (por ejemplo `2026-11-26T10:15:30`). El frontend no
muestra dos fechas sueltas: ofrece `Cualquier fecha`, `Hoy`, `Últimos 7 días`, `Últimos 30 días`
y `Elegir un rango`; solo en este último caso aparecen `Desde` y `Hasta`, convertidas a días
completos (`T00:00:00` / `T23:59:59`).

## Frontend

- `src/services/auditoriaService.js`: cliente HTTP.
- `src/pages/AuditoriaPage.jsx`: listado con filtros, paginación, detalle en tabla de
  `campo / anterior / nuevo`, alta manual con filas y botón **Corregir** en las filas manuales
  (el objeto se arma en el cliente, sin pedirle JSON al usuario).
- Ruta `/auditoria` dentro de `RutaProtegida soloAdmin` y enlace en el menú de administrador.

## Autenticación

El contexto dice JWT, pero el proyecto ya funciona con **sesión HTTP (cookie HttpOnly, 8 h)**
implementada en `usuario` y `common/security`. Cambiar a JWT habría significado tocar esos
módulos, así que se mantuvo el mecanismo existente y la auditoría solo consume la sesión.

## Tests

- `AuditoriaRepositoryTest`: JSONB, alta/modificación/baja automáticas, login y filtros
  (PostgreSQL 17 con Testcontainers).
- `AuditoriaServiceTest`: permisos (401/403), saneado de datos sensibles, actor `SISTEMA` y
  corrección que conserva la fila original.
- `AuditoriaControllerTest`: rutas, serialización del JSON, código 201 y cuerpo de una corrección.

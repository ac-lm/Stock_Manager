# Backlog — Módulo Costos

Tickets del módulo `costos` escritos como rebanadas verticales: cada uno incluye base de datos,
backend y pantalla cuando corresponde, y al terminarlo se puede probar de punta a punta. Están
en orden; **Bloqueado por** indica qué tiene que estar terminado antes.

**Decisiones vigentes:**

- Un **costo** es todo gasto del taller: insumos, sueldos, servicios y otros, con monto, fecha de
  pago, período (mes y año) y tipo.
- Los **tipos de costo** son una tabla administrable. El tipo **Insumos** es `reservado`: lo maneja
  el sistema, no se edita ni se desactiva, y no se puede usar para cargar un costo a mano.
- Cuando entra mercadería de un proveedor, el ingreso genera automáticamente un costo de tipo
  Insumos por el total. Ese costo no se edita ni se anula a mano.
- Los costos se pueden **anular** (baja lógica) con motivo, fecha y usuario. No se borran.
- Solo el rol **ADMINISTRADOR** puede acceder a `/api/costos/**` y `/api/tipos-costo/**`.
- El usuario de alta y el de anulación se toman de la sesión actual, nunca del cuerpo del request.
- Los totales **nunca** incluyen costos anulados.

**Convención de nombres:** los identificadores (`id_costo`, `id_tipo_costo`, `id_usuario`,
`id_usuario_anulacion`, `id_ingreso_mercaderia`) van en `snake_case`; el resto de campos, métodos y
claves JSON van en `camelCase`. Las columnas de la base conservan `snake_case` mediante
`@Column(name = ...)`.

> El documento [`04-costos-res.md`](04-costos-res.md) quedó como registro de preguntas; este
> backlog refleja lo que efectivamente se implementó.

Leyenda de estado: ✅ Hecho · 🟡 En curso · 🟢 Listo · 🔴 Bloqueado.

## Resumen

| ID | Ticket | Bloqueado por | Estado |
|---|---|---|---|
| [CST-01](CST-01-modelo-costo.md) | Modelo de costo y tipo de costo | — | ✅ |
| [CST-02](CST-02-crud-tipos-de-costo.md) | CRUD de tipos de costo | 01 | ✅ |
| [CST-03](CST-03-registro-manual.md) | Registro manual de costos | 01, 02, `USU-01` | ✅ |
| [CST-04](CST-04-edicion-y-anulacion.md) | Edición y anulación de costos | 03 | ✅ |
| [CST-05](CST-05-integracion-caja.md) | Integración con caja | 04, módulo `caja` | ✅ |
| [CST-06](CST-06-costo-desde-ingreso.md) | Costo automático desde el ingreso de mercadería | 01, módulo de ingresos | 🔴 |
| [CST-07](CST-07-consultas-y-totales.md) | Consultas, filtros y totales | 03 | ✅ |
| [CST-08](CST-08-auditoria-de-monto.md) | Auditar cambios de monto | 04 | 🟢 |
| [CST-09](CST-09-tests.md) | Tests del módulo | 03, 04, 07 | ✅ |
| [CST-10](CST-10-documentacion-swagger.md) | Documentar la API en Swagger | 03, 04, 07 | ✅ |
| [CST-11](CST-11-frontend-costos.md) | Pantalla de costos | 02, 03, 04, 07 | ✅ |

## Orden recomendado

1. `CST-01`.
2. `CST-02`.
3. `CST-03`.
4. **En paralelo:** `CST-04` y `CST-07`.
5. **En paralelo:** `CST-09` y `CST-10`.
6. `CST-11`.
7. `CST-08`.
8. `CST-05` (hecho) y `CST-06` cuando exista el módulo de ingresos de mercadería.

**Camino crítico:** `CST-01 → CST-02 → CST-03 → CST-04 → CST-11`.

## Prerrequisitos de otros módulos

- `common`: formato de errores `ErrorResponse`, excepciones de negocio y `TextoUtil`.
- `usuario`: `UsuarioService.obtenerAutenticado()` (`USU-01`) para registrar quién carga y anula.
- PostgreSQL con las tablas `Costo` y `TipoCosto` (se crean/actualizan con Hibernate).
- Para `CST-05`: módulo `caja` con el vínculo `id_costo` en los egresos (ya disponible).
- Para `CST-06`: módulo de ingresos de mercadería y stock.

## Verificación del módulo

```bash
cd backend
./mvnw -o test -Dtest='com.taller.gestion.costos.**,UsuarioServiceAutenticadoTest'
```

Verificación del frontend (pantalla de costos):

```bash
cd frontend
npm run lint
npm run build
```

Prueba manual en una base limpia:

1. Arrancar backend y frontend e iniciar sesión como administrador.
2. Crear un tipo de costo, editarlo y desactivarlo.
3. Verificar que el tipo Insumos figura como reservado y no se puede editar.
4. Cargar un costo manual con período y fecha de pago.
5. Filtrar por tipo, mes y año, y comprobar el total.
6. Editar el costo y anularlo con un motivo.
7. Verificar que el costo anulado no suma al total y no se puede volver a editar ni anular.
8. Entrar con un vendedor y comprobar que recibe 403 en `/api/costos/**` y `/api/tipos-costo/**`.
9. Registrar un egreso de caja con `id_costo` y comprobar que anular ese costo devuelve 409 con
   `"anulá primero el egreso"`.

## Fuera de alcance

- Generación automática del costo de insumos desde el ingreso de mercadería (ver `CST-06`).
- Liquidación de sueldos por persona: se registra un total por período con observación.
- Reportes y evolución del costo de insumos (corresponden al módulo `reportes`).

# Backlog — Módulo Clientes

Tickets del módulo `clientes` escritos como rebanadas verticales: cada uno incluye base de datos,
backend y pantalla cuando corresponde, y al terminarlo se puede probar de punta a punta. Están
en orden; **Bloqueado por** indica qué tiene que estar terminado antes.

**Decisiones vigentes:**

- Dos tipos de cliente de negocio: `GRAN_CLIENTE` y `CONSUMIDOR_FINAL`. Existe además un cliente
  `GENERICO` ("Consumidor final") reservado por el sistema, que no se crea, edita ni desactiva
  desde la API.
- El **CUIT** es obligatorio para grandes clientes y opcional para consumidores finales. Se
  normaliza a `XX-XXXXXXXX-X`, se valida el dígito verificador y es único.
- **Nombre**, **email** y **dirección** son obligatorios; teléfono y observaciones son opcionales.
- Las **condiciones comerciales** se implementaron como `plazoPagoDias` (plazo de pago en días,
  0 a 365), solo para grandes clientes. Esto reemplaza la propuesta de `descuento_pactado` del
  relevamiento: se descartó el descuento fijo y no hay cuenta corriente.
- La baja es **lógica** (`activo = false`): el cliente conserva su historial y deja de aparecer al
  vender. El cliente genérico no se puede desactivar.
- El cliente `GENERICO` lo crea el sistema al arrancar si no existe (`CLI-09`). Tiene `cuit` nulo;
  email y dirección son valores de sistema porque la entidad los exige. Otros módulos lo obtienen
  con `ClienteService.obtenerGenerico()`, que devuelve la entidad para tomar solo su `id_cliente`.

**Convención de nombres:** los identificadores (`id_cliente`, `id_usuario`, etc.) van en
`snake_case`; el resto de campos, métodos y claves JSON van en `camelCase`. Las columnas de la
base conservan `snake_case` mediante `@Column(name = ...)`.

> El documento [`03-clientes-res.md`](03-clientes-res.md) quedó como registro de preguntas; este
> backlog refleja lo que efectivamente se implementó.

Leyenda de estado: ✅ Hecho · 🟡 En curso · 🟢 Listo · 🔴 Bloqueado.

## Resumen

| ID | Ticket | Bloqueado por | Estado |
|---|---|---|---|
| [CLI-01](CLI-01-modelo-cliente.md) | Modelo y entidad de cliente | — | ✅ |
| [CLI-02](CLI-02-validacion-cuit.md) | Validación y normalización de CUIT | 01 | ✅ |
| [CLI-03](CLI-03-alta-y-edicion.md) | Alta y edición de clientes | 01, 02 | ✅ |
| [CLI-04](CLI-04-busqueda-y-similares.md) | Búsqueda y clientes similares | 01 | ✅ |
| [CLI-05](CLI-05-baja-logica-y-generico.md) | Baja lógica y cliente genérico | 01 | ✅ |
| [CLI-06](CLI-06-frontend-clientes.md) | Pantalla de clientes | 03, 04, 05 | ✅ |
| [CLI-07](CLI-07-tests.md) | Tests del módulo | 03, 04, 05 | ✅ |
| [CLI-08](CLI-08-documentacion-swagger.md) | Documentar la API en Swagger | 03, 04, 05 | ✅ |
| [CLI-09](CLI-09-cliente-generico-accesible.md) | Cliente genérico accesible para ventas | 05 | ✅ |

## Orden recomendado

1. `CLI-01`.
2. `CLI-02`.
3. **En paralelo:** `CLI-03`, `CLI-04` y `CLI-05`.
4. **En paralelo:** `CLI-06`, `CLI-07` y `CLI-08`.

**Camino crítico:** `CLI-01 → CLI-02 → CLI-03 → CLI-06`.

## Prerrequisitos de otros módulos

- `common`: formato de errores `ErrorResponse`, excepciones de negocio y `TextoUtil`.
- PostgreSQL con la tabla `Cliente` (se crea/actualiza con Hibernate).
- Frontend con React Router y el proxy de Vite `/api` → `http://localhost:8080`.

## Verificación del módulo

```bash
cd backend
./mvnw -o test -Dtest='com.taller.gestion.clientes.**'

cd ../frontend
npm run lint
npm run build
```

Prueba manual en una base limpia:

1. Arrancar backend y frontend.
2. Crear un gran cliente con CUIT válido y plazo de pago.
3. Intentar crear otro con el mismo CUIT: debe avisar el duplicado.
4. Buscar por nombre y por CUIT con y sin guiones.
5. Editar el cliente y pasarlo a consumidor final: debe pedir confirmación y borrar el plazo.
6. Desactivarlo, verlo atenuado con el badge **Inactivo** y reactivarlo.
7. Verificar que el cliente genérico figura como **Reservado** y no se puede editar ni desactivar.

## Fuera de alcance

- Cuenta corriente y descuento fijo pactado: se reemplazaron por el plazo de pago.
- Borrado físico de clientes con historial.
- Roles en la API de clientes: las anotaciones `@PreAuthorize` están declaradas, pero la
  seguridad por método todavía no está habilitada en el proyecto. Cuando se habilite, la idea es
  que administradores y vendedores puedan gestionar clientes.
- Auditoría de cambios de clientes (corresponde al módulo `auditoria`).

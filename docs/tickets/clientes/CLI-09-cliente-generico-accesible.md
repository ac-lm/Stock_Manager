# CLI-09. Cliente genérico accesible para ventas

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | `CLI-05` |
| Bloquea | `VEN-03` |
| Etiquetas | `clientes`, `backend`, `db`, `tests` |

**Historia:** como ventas, necesito obtener el cliente "Consumidor final" sin crear uno a mano para
que el alta de mostrador no frene.

## Qué construir

- `ClienteService.obtenerGenerico()` con acceso de solo lectura, que devuelva la entidad (igual que
  `ProductoService.obtenerPorId`) y falle con 404 si no está cargado.
- `ClienteRepository.findByTipoCliente(TipoCliente)`.
- `ClienteGenericoInitializer`, un `ApplicationRunner` idempotente que cree el cliente
  `GENERICO` al arrancar si no existe.

## Criterios de aceptación

- [x] Si no existe el genérico, se crea con nombre "Consumidor final", `tipoCliente = GENERICO`,
      `cuit = null` y `activo = true`.
- [x] Si ya existe, el arranque no vuelve a guardarlo.
- [x] Email y dirección se completan con valores de sistema porque la entidad los exige; no son
      datos de contacto reales.
- [x] `obtenerGenerico()` devuelve la entidad y no expone un endpoint nuevo.
- [x] Si no está cargado, `obtenerGenerico()` lanza `RecursoNoEncontradoException` (404).
- [x] El tipo `GENERICO` sigue sin poder crearse, editarse ni desactivarse desde la API.

## Tablas

`Cliente`. El DDL actual dice `cuit NOT NULL`; el modelo debe corregirse a nullable para que el
genérico pueda insertarse.

## Verificación

```bash
cd backend
./mvnw -Dtest=ClienteServiceTest,ClienteGenericoInitializerTest test
```

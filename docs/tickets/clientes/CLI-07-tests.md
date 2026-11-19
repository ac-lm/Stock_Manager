# CLI-07. Tests del módulo Clientes

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | `CLI-03`, `CLI-04`, `CLI-05` |
| Bloquea | — |
| Etiquetas | `clientes`, `tests` |

**Historia:** como desarrollador, quiero tests automáticos que cubran las reglas de negocio, la API
y el acceso a datos de clientes para poder refactorizar sin romper nada.

## Qué construir

- Tests de servicio, controller y repositorio.
- Tests unitarios del validador de CUIT.

## Criterios de aceptación

- [x] `CuitValidatorTest` cubre CUIT válidos, inválidos y formatos con guiones, puntos y espacios.
- [x] `ClienteServiceTest` cubre alta, edición, baja lógica, CUIT duplicado, tipo genérico y
      condiciones comerciales (16 tests).
- [x] `ClienteServiceBusquedaTest` cubre búsqueda por nombre, por CUIT sin guiones, inactivos y
      similares (5 tests).
- [x] `ClienteControllerTest` cubre códigos 200, 201, 400, 404 y 409, y la seguridad con
      `@WithMockUser` (9 tests).
- [x] `ClienteRepositoryTest` usa PostgreSQL con Testcontainers y cubre unicidad de CUIT,
      búsqueda y similares (7 tests).
- [x] Los tests de controller importan `SecurityConfig` y los handlers de seguridad, y mockean
      `UserDetailsService`.

## Verificación

```bash
cd backend
./mvnw -o test -Dtest='com.taller.gestion.clientes.**'
```

## Archivos

- `backend/src/test/java/com/taller/gestion/clientes/util/CuitValidatorTest.java`
- `backend/src/test/java/com/taller/gestion/clientes/service/ClienteServiceTest.java`
- `backend/src/test/java/com/taller/gestion/clientes/service/ClienteServiceBusquedaTest.java`
- `backend/src/test/java/com/taller/gestion/clientes/controller/ClienteControllerTest.java`
- `backend/src/test/java/com/taller/gestion/clientes/repository/ClienteRepositoryTest.java`

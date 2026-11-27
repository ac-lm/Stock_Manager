# CST-09. Tests del módulo Costos

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | `CST-03`, `CST-04`, `CST-07` |
| Bloquea | — |
| Etiquetas | `costos`, `tests` |

**Historia:** como desarrollador, quiero tests automáticos que cubran las reglas de negocio, la API
y el acceso a datos de costos para poder refactorizar sin romper nada.

## Qué construir

- Tests de servicio, controller y repositorio para costos y tipos de costo.
- Test del usuario autenticado usado por el módulo.

## Criterios de aceptación

- [x] `CostoServiceTest` cubre alta, edición, anulación, filtros, totales y reglas de tipo
      (17 tests).
- [x] `TipoCostoServiceTest` cubre alta, edición, estado, duplicados y tipo reservado (11 tests).
- [x] `TipoCostoInsumosInitializerTest` cubre la creación idempotente del tipo reservado Insumos y
      el conflicto de unicidad al arrancar (3 tests).
- [x] `UsuarioServiceAutenticadoTest` cubre la obtención del usuario de la sesión (3 tests).
- [x] `CostoControllerTest` cubre 200, 201, 400, 404, 409, 401 y 403 (15 tests).
- [x] `TipoCostoControllerTest` cubre 200, 201, 400, 404, 409, 401 y 403 (14 tests).
- [x] `CostoRepositoryTest` usa PostgreSQL con Testcontainers y cubre filtros, totales y vínculo
      con ingreso (4 tests).
- [x] `TipoCostoRepositoryTest` usa PostgreSQL con Testcontainers y cubre normalización y unicidad
      (3 tests).
- [x] Los tests de controller usan `@WithMockUser(roles = "ADMINISTRADOR")` y casos de 403 con
      vendedor.
- [x] `spring-security-test` está agregado al `pom.xml`.
- [x] `CostoAuditoriaIntegrationTest` (CST-08) verifica de punta a punta que el cambio de monto
      auditado por el listener global incluye anterior, nuevo, usuario y fecha, y que guardar el
      mismo monto no agrega otra fila (2 tests).

## Verificación

```bash
cd backend
./mvnw -o test -Dtest='com.taller.gestion.costos.**,UsuarioServiceAutenticadoTest'
# 73 tests: 70 del módulo costos + 3 de UsuarioServiceAutenticadoTest
```

Para correr solo la auditoría de monto:

```bash
./mvnw -o test -Dtest='CostoAuditoriaIntegrationTest'
```

> Los tests de repositorio levantan un contenedor PostgreSQL. Al apagar el contenedor, Surefire
> puede imprimir `Surefire is going to kill self fork JVM`; los tests igualmente pasan.

> El estado ✅ es para el alcance actual. `CST-05` ya tiene tests de integración y `CST-08` quedó
> cubierto con `CostoAuditoriaIntegrationTest`. Cuando se desbloquee `CST-06` habrá que agregar
> tests de creación, idempotencia y bloqueo de edición/anulación.

## Archivos

- `backend/src/test/java/com/taller/gestion/costos/service/CostoServiceTest.java`
- `backend/src/test/java/com/taller/gestion/costos/service/TipoCostoServiceTest.java`
- `backend/src/test/java/com/taller/gestion/costos/config/TipoCostoInsumosInitializerTest.java`
- `backend/src/test/java/com/taller/gestion/costos/controller/CostoControllerTest.java`
- `backend/src/test/java/com/taller/gestion/costos/controller/TipoCostoControllerTest.java`
- `backend/src/test/java/com/taller/gestion/costos/repository/CostoRepositoryTest.java`
- `backend/src/test/java/com/taller/gestion/costos/repository/TipoCostoRepositoryTest.java`
- `backend/src/test/java/com/taller/gestion/costos/integration/CostoAuditoriaIntegrationTest.java`
- `backend/src/test/java/com/taller/gestion/usuario/service/UsuarioServiceAutenticadoTest.java`

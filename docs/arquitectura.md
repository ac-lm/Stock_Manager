# Arquitectura y convenciones

## Stack

| Capa | Tecnología |
|---|---|
| Backend | Java 17, Spring Boot 3.5.16, Spring Data JPA, Maven |
| Base de datos | PostgreSQL 15+ |
| Frontend | React 19, Vite 8, React Router |
| Documentación API | Springdoc OpenAPI (Swagger UI) |
| Empaquetado | WAR para despliegue en Tomcat externo |

## Organización del backend

Cada módulo vive en `com.taller.gestion.<modulo>` con esta estructura interna:

```
<modulo>/
├── controller/   Endpoints REST
├── service/      Lógica de negocio y transacciones
├── repository/   Acceso a datos (solo lo usa el service del módulo)
├── entity/       Entidades JPA
├── dto/          Records de request y response
└── exception/    Excepciones propias del módulo
```

Módulos del sistema:

`usuario`, `auditoria`, `catalogo`, `proveedores`, `stock`, `incidencias`, `cliente`, `venta`, `pedido`, `caja`, `costos`, `reportes`, `common`.

El nombre del paquete va en singular (`usuario`, `venta`), aunque la ruta de la API use el plural
(`/api/usuarios`, `/ventas`).

`reportes` no tiene entidades propias: solo consultas y DTOs de salida.

## Reglas de desarrollo

1. **DTOs con `record`.** Todos los request/response del backend son `record` de Java 17. No se exponen entidades JPA desde los controllers.
2. **IDs `Long` y nomenclatura `id_<nombre>`.** Aplica a entidades, DTOs, JSON, `@PathVariable` y JPQL. Ejemplos: `id_insumo`, `id_producto_insumo`, `id_proveedor`.
3. **Los repositorios solo se acceden desde el service de su propio módulo.** Para usar datos de otro módulo se inyecta el **service** correspondiente, nunca su repository.
4. **Borrado.** La regla general es baja lógica cuando el modelo define un estado/booleano (`Cliente.activo`). En `catalogo` y `proveedores` se acordó **baja física** porque el modelo de Vertabelo no define columna de estado para esas tablas. Las relaciones hijas se eliminan en cascada.
5. **Transacciones.** Las operaciones de escritura usan `@Transactional`; las lecturas `@Transactional(readOnly = true)`.
6. **Validación y errores.** Cada módulo lanza excepciones propias con `@ResponseStatus`:
   - `400` datos inválidos,
   - `404` recurso no encontrado,
   - `409` duplicado.
   El manejo global de errores queda pendiente en el módulo `common`.
7. **Nombres de DTOs.** `XCreateDTO`, `XUpdateDTO`, `XResponseDTO`.
8. **Actualizaciones parciales.** Los `UpdateDTO` aplican solo los campos no nulos.
9. **Queries con guion bajo.** Cuando una propiedad tiene `_` (`id_proveedor`), Spring Data la interpreta como ruta anidada. En esos casos se usa `@Query` con JPQL explícito.
10. **Relaciones entre módulos.** Se modelan con `@ManyToOne` a la entidad del otro módulo (por ejemplo `ProductoInsumo.insumo` → `Insumo`).

## Convenciones REST

| Acción | Método | Respuesta |
|---|---|---|
| Listar | `GET /api/...` | `200` + lista de DTOs |
| Obtener | `GET /api/.../{id}` | `200` + DTO |
| Crear | `POST /api/...` | `201` + DTO |
| Actualizar | `PUT /api/.../{id}` | `200` + DTO |
| Eliminar | `DELETE /api/.../{id}` | `204` |

Los recursos dependientes se exponen anidados: `/api/catalogo/productos/{id_producto}/insumos`.

## Endpoints implementados

### Catálogo

| Método | Ruta |
|---|---|
| GET / POST | `/api/catalogo/insumos` |
| GET / PUT / DELETE | `/api/catalogo/insumos/{id_insumo}` |
| GET / POST | `/api/catalogo/productos` |
| GET / PUT / DELETE | `/api/catalogo/productos/{id_producto}` |
| GET / POST | `/api/catalogo/productos/{id_producto}/insumos` |
| PUT / DELETE | `/api/catalogo/productos/{id_producto}/insumos/{id_producto_insumo}` |

### Proveedores

| Método | Ruta |
|---|---|
| GET / POST | `/api/proveedores` |
| GET / PUT / DELETE | `/api/proveedores/{id_proveedor}` |
| GET / POST | `/api/proveedores/{id_proveedor}/insumos` |
| DELETE | `/api/proveedores/{id_proveedor}/insumos/{id_proveedor_insumo}` |

### Incidencias

| Método | Ruta |
|---|---|
| POST | `/api/incidencias` |

## Frontend

- `src/services/` contiene el cliente HTTP y un servicio por módulo.
- `src/pages/` contiene una pantalla por módulo.
- Las pantallas de catálogo y proveedores consumen la API real.
- El proxy de Vite evita CORS en desarrollo. Para producción o Tomcat externo habrá que configurar CORS en el módulo `common`.

## CI

`.github/workflows/ci.yml` se ejecuta en push/PR a `develop` y `main`:

- Backend contra PostgreSQL 15 y 17 (`./mvnw verify`).
- Despliegue del WAR en Tomcat 10.1 y 11.
- Frontend con Node 22 y 26 (`npm ci && npm run build`).

Antes de abrir un PR conviene correr localmente:

```bash
cd backend && ./mvnw verify
cd frontend && npm run lint && npm run build
```

## Pendientes técnicos

- Módulo `common`: manejo global de excepciones, formato único de error y configuración de CORS.
- Módulo `usuarios`: login, roles y permisos.
- Migraciones Flyway (las dependencias ya están en el `pom.xml`, pero todavía no hay migraciones).
- Módulos de negocio restantes: stock, clientes, ventas, pedidos, caja, costos, auditoría y reportes.

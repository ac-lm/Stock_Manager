# Stock Manager

Sistema de gestión de stock para taller. Proyecto final de la Práctica Profesional Supervisada (PPS).

- **Backend:** Java 17 + Spring Boot 3.5.16 + PostgreSQL + Maven (empaquetado WAR).
- **Frontend:** React 19 + Vite 8 + React Router.
- **Base de datos:** PostgreSQL 15+.
- **Documentación de API:** Springdoc OpenAPI (Swagger UI).

## Estructura del repositorio

```
Stock_Manager/
├── backend/        API REST (Spring Boot)
├── frontend/       Aplicación web (React + Vite)
├── docs/           Documentación técnica y de arquitectura
└── .github/        CI (GitHub Actions)
```

## Puesta en marcha

### Requisitos

- Java 17
- Node.js >= 22.13
- Docker (para PostgreSQL) o una instancia de PostgreSQL local

### 1. Base de datos

```bash
cd backend
docker compose up -d
```

Esto levanta PostgreSQL en `localhost:5432` con:

- Base: `stock_db`
- Usuario: `postgres`
- Contraseña: `postgres`

### 2. Backend

```bash
cd backend
./mvnw spring-boot:run
```

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

En desarrollo Hibernate crea/actualiza las tablas con `spring.jpa.hibernate.ddl-auto=update`.

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

- Aplicación: `http://localhost:5173`
- Vite redirige `/api` a `http://localhost:8080` (no hace falta configurar CORS en desarrollo).

## Estado de los módulos

| Módulo | Estado |
|---|---|
| `catalogo` | CRUD completo de Insumo, Producto y Producto_Insumo |
| `proveedores` | CRUD completo de Proveedor y Proveedor_Insumo |
| `incidencias` | Alta básica de incidencias |
| `venta` | Entidad `Venta` y estructura inicial |
| `usuario` | Entidades `Usuario` y `Rol`, sin login ni seguridad |
| `auditoria`, `stock`, `clientes`, `pedidos`, `caja`, `costos`, `reportes`, `common` | Pendientes |

## Documentación

- [`docs/arquitectura.md`](docs/arquitectura.md): módulos, reglas de desarrollo y convenciones.
- [`docs/modelo-datos.md`](docs/modelo-datos.md): tablas y decisiones sobre el modelo relacional.
- [`docs/db/README.md`](docs/db/README.md): cómo versionar el modelo de Vertabelo.

## Convenciones rápidas

- Un paquete por módulo dentro de `com.taller.gestion`.
- DTOs con `record` de Java 17.
- IDs `Long` con nomenclatura `id_<nombre>`.
- Los repositorios solo se usan desde el servicio de su propio módulo.
- Commits con [Conventional Commits](https://www.conventionalcommits.org/): `feat(modulo): ...`, `fix(modulo): ...`, `docs: ...`.
- Rama de trabajo: `develop`. La CI se ejecuta en cada push/PR.

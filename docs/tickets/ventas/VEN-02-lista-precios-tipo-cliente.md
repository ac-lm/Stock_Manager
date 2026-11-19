# VEN-02. Lista de precios por tipo de cliente

| Campo | Valor |
|---|---|
| Estado | 🟡 En curso |
| Bloqueado por | Modelo de datos (tabla pendiente de agregar) |
| Bloquea | `VEN-03` (resuelto con fallback) |
| Etiquetas | `ventas`, `backend`, `db`, `diseño` |

**Historia:** como cliente grande, quiero que mis ventas usen mi lista de precios, sin que el
vendedor tenga que cambiar el precio a mano.

## Qué construir

- Entidad `Precio_Tipo_Cliente` con `id_producto`, `tipo_cliente` y `precio decimal(12,2)`.
- Resolución de precio en `VentaService`: lista del tipo de cliente, con fallback a
  `Producto.precio` mientras no haya filas cargadas.
- **Pendiente:** CRUD para cargar y mantener los precios, y agregar la tabla al modelo.

## Criterios de aceptación

- [x] Prevalece la decisión 2b sobre "precio de catálogo con descuento".
- [x] El precio nunca se recibe en el body del alta.
- [x] Si no hay fila para ese producto/tipo, se usa el precio de catálogo.
- [ ] Unicidad `(id_producto, tipo_cliente)`.
- [ ] La tabla existe en el export del modelador.
- [ ] Alta/edición/baja de precios (¿en `catalogo`, `clientes` o `venta`?).

## Tablas

`Precio_Tipo_Cliente` (hoy creada por JPA con `ddl-auto=update`, **no está en el DDL**).

> SQL a agregar al modelo:
>
> ```sql
> CREATE TABLE Precio_Tipo_Cliente (
>     id_precio_tipo_cliente serial NOT NULL,
>     id_producto int NOT NULL,
>     tipo_cliente varchar(20) NOT NULL,
>     precio decimal(12,2) NOT NULL,
>     CONSTRAINT Precio_Tipo_Cliente_pk PRIMARY KEY (id_precio_tipo_cliente),
>     CONSTRAINT uk_precio_tipo_cliente UNIQUE (id_producto, tipo_cliente)
> );
> ```

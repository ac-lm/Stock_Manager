# CLI-06. Pantalla de clientes

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | `CLI-03`, `CLI-04`, `CLI-05` |
| Bloquea | — |
| Etiquetas | `clientes`, `frontend` |

**Historia:** como administrador o vendedor, quiero una pantalla única para listar, buscar, crear,
editar y dar de baja clientes.

## Qué construir

- Página **Clientes** con tabla, buscador, filtro de inactivos, paginación y acciones.
- Modal de alta/edición y aviso de clientes similares.
- Servicio `clientesService` para consumir la API.

## Criterios de aceptación

- [x] La tabla muestra nombre, CUIT, tipo, teléfono, email, plazo de pago, estado y acciones.
- [x] El buscador consulta por nombre o CUIT y el checkbox **Mostrar inactivos** incluye las bajas.
- [x] La paginación muestra el total de elementos y permite avanzar y retroceder.
- [x] El botón **Nuevo cliente** abre el modal vacío con tipo `CONSUMIDOR_FINAL` por defecto.
- [x] El modal valida los campos obligatorios y muestra los errores de la API.
- [x] Al escribir un nombre de al menos 3 letras se muestran los clientes similares; al elegir uno
      se abre para editar en lugar de crear un duplicado.
- [x] El plazo de pago solo se muestra y se envía para grandes clientes; sin plazo se muestra
      **Contado**.
- [x] Al guardar se muestra un banner de éxito y se recarga el listado.
- [x] **Desactivar** y **Reactivar** actualizan el estado y recargan el listado.
- [x] Los clientes inactivos se ven atenuados y con el badge **Inactivo**.
- [x] El cliente genérico se muestra como **Reservado** y sin acciones.
- [x] Las claves JSON usan `id_cliente` para el identificador y `camelCase` para el resto
      (`tipoCliente`, `plazoPagoDias`).

## Archivos

- `frontend/src/pages/ClientesPage.jsx`
- `frontend/src/services/clientesService.js`

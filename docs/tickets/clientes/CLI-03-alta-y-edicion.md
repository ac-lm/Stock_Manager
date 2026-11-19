# CLI-03. Alta y edición de clientes

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | `CLI-01`, `CLI-02` |
| Bloquea | `CLI-06`, `CLI-07`, `CLI-08` |
| Etiquetas | `clientes`, `backend`, `frontend` |

**Historia:** como administrador o vendedor, quiero dar de alta y editar clientes para mantener
actualizados sus datos y condiciones comerciales.

## Qué construir

- `POST /api/clientes` y `PUT /api/clientes/{id_cliente}`.
- DTO de alta, de edición y de respuesta.
- Modal **Nuevo cliente** / **Editar cliente** en la pantalla de clientes.

## Criterios de aceptación

- [x] El alta pide `cuit`, `nombre`, `telefono`, `email`, `direccion`, `tipoCliente`,
      `observaciones` y `plazoPagoDias`.
- [x] `nombre` (máximo 50), `email` (válido, máximo 150) y `direccion` (máximo 80) son
      obligatorios; `telefono` (30) y `observaciones` (250) son opcionales.
- [x] El plazo de pago acepta valores entre 0 y 365 días.
- [x] El plazo solo se admite para `GRAN_CLIENTE`; en otro tipo responde 400.
- [x] Al editar un gran cliente y pasarlo a consumidor final, el plazo se limpia
      automáticamente.
- [x] El alta responde 201 y la edición 200 con `ClienteResponseDTO`.
- [x] Un id inexistente responde 404.
- [x] El cliente `GENERICO` no se puede modificar.
- [x] Los errores de validación responden 400 con el detalle por campo.
- [x] El modal reutiliza el mismo formulario para alta y edición y muestra los errores de la API.
- [x] Si el cambio de tipo hace perder las condiciones comerciales, el frontend pide
      confirmación antes de guardar.

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/clientes` | Crea un cliente y devuelve 201 |
| `PUT` | `/api/clientes/{id_cliente}` | Modifica un cliente existente |

## Tablas

`Cliente`.

# CST-04. Edición y anulación de costos

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | `CST-03` |
| Bloquea | `CST-05`, `CST-08`, `CST-09`, `CST-10`, `CST-11` |
| Etiquetas | `costos`, `backend` |

**Historia:** como administrador, quiero corregir un costo mal cargado o anularlo sin borrarlo,
dejando registro de quién y por qué lo anuló.

## Qué construir

- `PUT /api/costos/{id_costo}` para editar.
- `PATCH /api/costos/{id_costo}/anulacion` para anular.
- `CostoUpdateDTO` y `CostoAnularDTO`.

## Criterios de aceptación

- [x] La edición aplica las mismas validaciones que el alta.
- [x] La edición **no** cambia el usuario de alta original.
- [x] Un id inexistente responde 404.
- [x] No se puede editar un costo anulado (409).
- [x] No se puede editar un costo generado por un ingreso de mercadería (409).
- [x] La anulación pide un `motivo` obligatorio de hasta 255 caracteres.
- [x] La anulación guarda `anulado = true`, el motivo, la fecha y hora, y el usuario de la sesión.
- [x] No se puede anular un costo ya anulado (409).
- [x] No se puede anular un costo generado por un ingreso (409).
- [x] Ambas operaciones responden 200 con `CostoResponseDTO`.

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| `PUT` | `/api/costos/{id_costo}` | Modifica un costo |
| `PATCH` | `/api/costos/{id_costo}/anulacion` | Anula un costo con un motivo |

## Tablas

`Costo`, `TipoCosto`, `Usuario`.

## Pendientes relacionados

- `CST-05`: ✅ implementado; la anulación rechaza costos vinculados a un egreso de caja.
- `CST-08`: auditar el monto anterior y el nuevo.

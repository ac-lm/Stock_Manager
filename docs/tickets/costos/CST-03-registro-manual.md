# CST-03. Registro manual de costos

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | `CST-01`, `CST-02`, `USU-01` |
| Bloquea | `CST-04`, `CST-07`, `CST-09`, `CST-10`, `CST-11` |
| Etiquetas | `costos`, `backend` |

**Historia:** como administrador, quiero registrar un gasto con su tipo, monto, fecha de pago y
período para llevar el control de costos del taller.

## Qué construir

- `POST /api/costos` con validaciones.
- `CostoCreateDTO` y `CostoResponseDTO`.
- Obtención del usuario autenticado desde la sesión.

## Criterios de aceptación

- [x] El body pide `id_tipo_costo`, `descripcion`, `monto`, `fechaPago`, `periodoMes`,
      `periodoAnio` y `observaciones` opcional.
- [x] `descripcion` es obligatoria (máximo 255) y `monto` es obligatorio, positivo y con hasta 2
      decimales.
- [x] `fechaPago` es obligatoria; `periodoMes` va de 1 a 12 y `periodoAnio` de 2000 a 2100.
- [x] El tipo debe existir, estar activo y no ser reservado; si no, responde 400.
- [x] El costo se crea con `anulado = false` y el usuario de alta tomado de
      `UsuarioService.obtenerAutenticado()`.
- [x] La respuesta es 201 con `CostoResponseDTO`, que expone ids en lugar de entidades.
- [x] Los errores de validación responden 400 con el detalle por campo.
- [x] Sin sesión responde 401; con rol vendedor responde 403.

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/costos` | Registra un costo manual y devuelve 201 |

## Tablas

`Costo`, `TipoCosto`, `Usuario`.

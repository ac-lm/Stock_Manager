# CLI-01. Modelo y entidad de cliente

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | — |
| Bloquea | `CLI-02` a `CLI-08` |
| Etiquetas | `clientes`, `db`, `backend` |

**Historia:** como sistema, necesito persistir los clientes del taller con sus datos de contacto y
su tipo, para poder asociarlos a las ventas y estadísticas.

## Qué construir

- Tabla `Cliente` y enum `TipoCliente`.
- Entidad JPA `Cliente` con los campos y restricciones del modelo.

## Criterios de aceptación

- [x] La PK es `id_cliente` autoincremental; el CUIT **no** es la PK (un CUIT de 11 dígitos no
      entra en un `int` y el consumidor final puede no tener CUIT).
- [x] `cuit` es `varchar(13)`, opcional y único.
- [x] `nombre` es obligatorio (50 caracteres), `email` obligatorio (150) y `direccion` obligatoria
      (80); `telefono` (30) y `observaciones` (250) son opcionales.
- [x] `tipoCliente` es un enum obligatorio con `GRAN_CLIENTE`, `CONSUMIDOR_FINAL` y `GENERICO`.
- [x] `activo` es booleano y arranca en `true`.
- [x] `plazoPagoDias` es opcional y representa las condiciones comerciales del gran cliente.
- [x] `nombreNormalizado` es obligatorio y se calcula en `@PrePersist` / `@PreUpdate` con
      `TextoUtil.normalizarParaBusqueda` para buscar sin distinguir mayúsculas ni acentos.
- [x] Las columnas usan `snake_case` (`id_cliente`, `tipo_cliente`, `plazo_pago_dias`,
      `nombre_normalizado`) y los campos Java usan `camelCase`, salvo `id_cliente`.

## Tablas

`Cliente`.

## Notas

- El relevamiento proponía `descuento_pactado`; se reemplazó por `plazoPagoDias` al no existir
  cuenta corriente.
- El cliente `GENERICO` se inserta como dato inicial del sistema y queda reservado.

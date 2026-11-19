# CST-01. Modelo de costo y tipo de costo

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | — |
| Bloquea | `CST-02` a `CST-10` |
| Etiquetas | `costos`, `db`, `backend` |

**Historia:** como sistema, necesito persistir los costos del taller y sus tipos para poder
registrar gastos por período y calcular totales.

## Qué construir

- Tablas `Costo` y `TipoCosto`.
- Entidades JPA `Costo` y `TipoCosto`.

## Criterios de aceptación

- [x] `Costo` tiene PK `id_costo` autoincremental.
- [x] `Costo.tipoCosto` es una relación `@ManyToOne` obligatoria contra `TipoCosto`.
- [x] `Costo` guarda `descripcion` (255), `monto` (`decimal(12,2)`), `fechaPago`,
      `periodoMes`, `periodoAnio` y `observaciones` (255).
- [x] `Costo.usuario` es una relación `@ManyToOne` obligatoria contra `Usuario` (quien cargó el
      costo).
- [x] `Costo` guarda la baja lógica con `anulado`, `motivoAnulacion`, `fechaAnulacion` y
      `usuarioAnulacion` (opcional).
- [x] `Costo.id_ingreso_mercaderia` es opcional y único: identifica los costos generados por un
      ingreso de mercadería.
- [x] `TipoCosto` tiene PK `id_tipo_costo`, `nombre` (50) y `nombreNormalizado` (50, único) que se
      calcula en `@PrePersist` / `@PreUpdate`.
- [x] `TipoCosto` tiene `activo` (por defecto `true`) y `reservado` (por defecto `false`); el tipo
      Insumos se marca como reservado.
- [x] `TipoCostoInsumosInitializer` crea el tipo reservado **Insumos** al arrancar si no existe
      (`ApplicationRunner` idempotente), para que los ingresos de mercadería tengan un tipo al
      cual asociar su costo.
- [x] Las columnas usan `snake_case` (`id_costo`, `id_tipo_costo`, `fecha_pago`, `periodo_mes`,
      `periodo_anio`, `motivo_anulacion`, `usuario_anulacion`) y los campos Java usan `camelCase`,
      salvo los identificadores.

## Tablas

`Costo`, `TipoCosto`.

## Notas

- `usuarioAnulacion` se llamaba `usuario_anulacion`; se renombró para respetar la convención
  (solo los ids quedan en `snake_case`).
- Las columnas de la base no cambiaron de nombre para no romper datos existentes.

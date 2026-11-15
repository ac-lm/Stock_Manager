# Módulo Incidencias: preguntas (grill-me)

**Cómo completarlo:** marquen una opción (o escriban otra) en "Respuesta". ★ = la que recomiendo, con el motivo. **(clienta)** = probablemente solo la sepa ella.

**Relacionado con Stock:** pregunta 7 (incidencia vs salida por daño).

---

## 1. (clienta) ¿Qué tipos de incidencia hay?

- c) Lista editable por el administrador.

**Respuesta:** c) Lista editable por el administrador.

**Impacto:** el tipo **no puede ser un enum**. Necesita una tabla `Tipo_Incidencia` y un CRUD para que el administrador agregue, edite o desactive tipos.

---

## 2. ¿Qué estados tiene una incidencia?

- b) Solo Abierta / Cerrada.

**Respuesta:** b) Solo Abierta / Cerrada.

**Impacto:** enum `EstadoIncidencia { ABIERTA, CERRADA }`. Hay que reemplazar el estado actual `PENDIENTE` y migrar los registros existentes a `ABIERTA`.

---

## 3. ¿Una incidencia puede no afectar stock? (por ejemplo, una máquina rota)

- b) No, siempre afecta un producto o insumo.

**Respuesta:** b) No, siempre afecta un producto o insumo.

**Impacto:** se mantiene la regla **"al menos uno de producto o insumo"** (`CHECK` en base y validación en service). Además, hay que habilitar la relación con `Producto`, que hoy está comentada en la entidad.

---

## 4. Si afecta stock, ¿cuándo se descuenta?

- b) Al resolverla.

**Respuesta:** b) Al resolverla.

**Impacto:** al pasar de `ABIERTA` a `CERRADA` se descuenta stock en la misma transacción. Si no alcanza el stock, la resolución falla completa (409) y la incidencia queda abierta. **Depende del módulo `stock`**, que todavía no existe.

---

## 5. ¿Quién registra y quién resuelve?

- b) Cualquiera hace todo.

**Respuesta:** b) Cualquiera hace todo.

**Impacto:** no se agregan restricciones por rol en este módulo. Igual se registra **quién creó** (`id_usuario`) y **quién resolvió** (`id_usuario_resolucion`) para auditoría.

---

## 6. ¿Una incidencia puede estar ligada a una venta o un pedido? (reclamo de un cliente)

- b) No.

**Respuesta:** b) No.

**Impacto:** no se agrega `id_venta` ni `id_pedido`. Se descarta la opción 6a.

---

## Notas del diagrama

- `Incidencia.id_usuario` no está marcado como FK.
- `cantidad_afectada` es `int`: `decimal(12,3)`.
- Agregar `fecha` (el PDF la pide), `id_usuario_resolucion` y `fecha_resolucion`, y `id_venta` nula si eligen 6a.
- `tipo_incidencia` y `estado` como enum.
- El CHECK "al menos uno de producto o insumo" sale si eligen 3a.

### Cómo quedan esas notas con las respuestas elegidas

| Nota original | Decisión final |
|---|---|
| `id_usuario` no es FK | Se agrega `@ManyToOne Usuario` con FK `id_usuario` (**not null**). |
| `cantidad_afectada` es `int` | Pasa a `numeric(12,3)` / `BigDecimal`. |
| Agregar `fecha`, `id_usuario_resolucion`, `fecha_resolucion` | Se agregan. `fecha` y `fecha_resolucion` se modelan como `LocalDateTime`. |
| Agregar `id_venta` si eligen 6a | **No se agrega**: la respuesta fue 6b. |
| `tipo_incidencia` y `estado` como enum | `estado` sí (`ABIERTA`/`CERRADA`). `tipo_incidencia` **no**: la respuesta 1c pide lista editable, entonces va a tabla `Tipo_Incidencia`. |
| CHECK "al menos uno de producto o insumo" | **Se mantiene**, porque la respuesta fue 3b (siempre afecta uno). |

---

## Decisiones técnicas derivadas

| Pregunta | Respuesta | Decisión |
|---|---|---|
| 1 | c | Tabla `Tipo_Incidencia` + CRUD. `Incidencia.id_tipo_incidencia` como FK. |
| 2 | b | Enum `EstadoIncidencia` con `ABIERTA` y `CERRADA`. |
| 3 | b | `id_producto` e `id_insumo` nullables, con `CHECK (id_producto IS NOT NULL OR id_insumo IS NOT NULL)`. |
| 4 | b | Descuento de stock dentro de la transacción de resolución. |
| 5 | b | Sin chequeo de rol. Se guardan `id_usuario` y `id_usuario_resolucion`. |
| 6 | b | Sin relación con venta/pedido. |

---

## Conflictos y supuestos a confirmar

1. **Tipo editable vs. enum (Q1 vs. nota del diagrama).** Las dos cosas no pueden ser ciertas. Tomamos la respuesta 1c: tabla `Tipo_Incidencia`. Esto obliga a **actualizar el modelo de Vertabelo** antes de codificar.
2. **Tipo de dato de `fecha`.** El PDF no lo define. Se asume `LocalDateTime` (fecha + hora del registro). Si el negocio solo necesita el día, cambiar a `LocalDate`.
3. **"Al menos uno" vs. "exactamente uno".** La nota dice "al menos uno". Se mantiene así: una incidencia podría afectar un producto y un insumo a la vez. Si debe ser excluyente, definirlo antes de INC-01.
4. **Descuento cuando hay producto e insumo.** Si se permiten ambos, al cerrar se descuenta stock de los dos. Confirmar si ese es el comportamiento esperado.
5. **"Administrador" para el CRUD de tipos.** Hasta que exista `usuarios` con login, no hay forma de restringir el CRUD por rol. Se implementa sin restricción y se protege cuando esté la seguridad.
6. **Stock insuficiente al cerrar.** La regla del proyecto es que el stock nunca sea negativo. Se rechaza la resolución con 409 y se mantiene la incidencia `ABIERTA`.

---

## Qué existe hoy en el código

- `Incidencia` tiene `id_incidencia`, relación con `Insumo`, `tipoIncidencia` (String), `descripcion`, `cantidadAfectada` (`Integer`), `estado` (String, se crea como `"PENDIENTE"`), `observaciones` y `resolucionAdoptada`.
- La relación con `Producto` está comentada.
- No tiene `fecha`, `id_usuario` ni datos de resolución.
- Solo existe `POST /api/incidencias`.
- `InsumoService.obtenerPorId` y `ProductoService.obtenerPorId` ya existen y se pueden reutilizar.
- `UsuarioService` está vacío: no hay forma de obtener un usuario por ID.
- No existe el módulo `stock`.
- `common` ya tiene el manejo global de excepciones.

# CST-08. Auditar cambios de monto

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | `CST-04` |
| Bloquea | — |
| Etiquetas | `costos`, `backend`, `auditoria` |

**Historia:** como administrador, quiero saber cuándo y cuánto cambió el monto de un costo para
detectar correcciones o errores de carga.

## Qué construir

- Registro de auditoría del monto anterior y el nuevo al editar un costo.
- Reutilizar el mecanismo del módulo `auditoria` o, si no existe, dejar el evento preparado.

## Criterios de aceptación

- [x] Al modificar el monto de un costo se registra el valor anterior y el nuevo.
- [x] La auditoría incluye el usuario que hizo el cambio y la fecha y hora.
- [x] Si el monto no cambia, no se genera un registro de auditoría de monto.
- [x] La edición sigue funcionando igual para el cliente de la API.
- [x] Hay tests que cubren el cambio de monto y el caso sin cambio.

## Cómo se resolvió

No hizo falta código nuevo en `CostoService` ni tocar el listener compartido
`AuditoriaEntityListener`. El listener global de Hibernate (`auditoria`) ya registra altas,
modificaciones y bajas de cualquier entidad JPA, así que el `UPDATE` que dispara el *dirty
checking* al editar un costo escribe por sí solo la fila de auditoría:

- `accion = MODIFICACION`, `tabla = "Costo"`, `id_registro = id_costo`.
- `cambios.monto.anterior` y `cambios.monto.nuevo`, con nombres de columna.
- `id_usuario` y `usuario` (email congelado) del usuario autenticado, y `fecha_hora` no nula.

`CostoService.actualizar` conserva el `TODO (CST-08)` como marcador histórico del punto de
extensión que se había previsto; hoy el evento se produce sin código en el servicio.

## Verificación

`CostoAuditoriaIntegrationTest` (PostgreSQL 17 con Testcontainers, 2 tests) cubre el cambio de
monto y el caso sin cambio, y comprueba que la edición sigue respondiendo 200 con el monto nuevo:

```bash
cd backend
./mvnw -o test -Dtest='CostoAuditoriaIntegrationTest'
```

El test prepara usuario, tipo y costo por JDBC y aísla `UsuarioService` con `@MockitoBean`: el
listener consulta el usuario dentro del flush y esa consulta modifica la cola de acciones de
Hibernate mientras se itera (`ConcurrentModificationException`). La edición se hace con
`PUT /api/costos/{id}`, que confirma su propia transacción igual que en producción.

## Pendiente de hablar con el equipo

`AuditoriaEntityListener` decide si un campo cambió con `Objects.equals`
(`AuditoriaEntityListener.java:69`), que en `BigDecimal` compara también la escala. En la práctica
Hibernate ya considera iguales `150.0` y `150.00` al hacer *dirty checking*
(`BigDecimalJavaType.areEqual` usa `compareTo`), así que ese caso no llega al listener y no genera
auditoría. La decisión acordada es que **un cambio solo de escala no cuenta como cambio de monto**.
No se modificó el listener compartido; queda pendiente de conversar con el equipo si conviene
hacer explícita la normalización en el listener (por ejemplo con `compareTo`) para no depender del
criterio de Hibernate, sin afectar a los demás módulos. El test usa escalas iguales
(`150.00 → 175.00`).

No se guarda IP: ni `Auditoria` ni `Costo` tienen ese campo y CST-08 no la exige.

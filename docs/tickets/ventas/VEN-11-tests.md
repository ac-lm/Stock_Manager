# VEN-11. Tests del módulo

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho (parcial: vuelve a abrirse con los tickets diferidos) |
| Bloqueado por | `VEN-03`, `VEN-04`, `VEN-05` |
| Bloquea | — |
| Etiquetas | `ventas`, `tests` |

**Historia:** como equipo, quiero que el alta, la anulación y las consultas estén cubiertas para no
romper el cálculo de totales.

## Qué construir

- Tests unitarios del service con Mockito.
- Tests de controller con MockMvc y seguridad.

## Criterios de aceptación

- [x] Alta de mostrador con cliente genérico y vendedor de sesión.
- [x] Precio por tipo de cliente y fallback a catálogo.
- [x] Cliente inactivo, sin productos y cantidad cero rechazados.
- [x] Total y subtotales calculados con `BigDecimal`.
- [x] Anulación y doble anulación.
- [x] Filtros del listado.
- [x] Endpoints: 201, 200, 400.
- [ ] Agregar tests de repositorio con Testcontainers.
- [ ] Agregar tests de los tickets diferidos (`VEN-06` a `VEN-10`).

## Verificación

```bash
cd backend
./mvnw -Dtest='com.taller.gestion.venta.**' test
```

Resultado actual: **15 tests, 0 fallos**.

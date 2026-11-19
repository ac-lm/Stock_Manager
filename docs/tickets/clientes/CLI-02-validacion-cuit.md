# CLI-02. Validación y normalización de CUIT

| Campo | Valor |
|---|---|
| Estado | ✅ Hecho |
| Bloqueado por | `CLI-01` |
| Bloquea | `CLI-03` |
| Etiquetas | `clientes`, `backend`, `tests` |

**Historia:** como administrador, quiero que el sistema valide el CUIT al cargar un cliente para
evitar datos inválidos y clientes duplicados.

## Qué construir

- `CuitValidator` con validación de formato y dígito verificador.
- Normalización del CUIT en `ClienteService` y control de unicidad.

## Criterios de aceptación

- [x] Se aceptan CUIT con números, guiones, puntos y espacios.
- [x] Se rechazan letras y otros caracteres con un error de datos inválidos.
- [x] Se exigen exactamente 11 dígitos.
- [x] Se valida el dígito verificador del CUIT.
- [x] El CUIT se guarda normalizado como `XX-XXXXXXXX-X`.
- [x] El CUIT es obligatorio para `GRAN_CLIENTE` y opcional para `CONSUMIDOR_FINAL`; vacío se
      guarda como `null`.
- [x] Un CUIT repetido responde 409 con `"Ya existe un cliente con ese CUIT"`.
- [x] Al editar, la unicidad excluye al propio cliente.
- [x] El tipo `GENERICO` no se acepta en altas ni ediciones.

## Componentes

`CuitValidator`, `ClienteService`, `ClienteRepository`.

## Verificación

```bash
cd backend
./mvnw -o test -Dtest=CuitValidatorTest,ClienteServiceTest
```

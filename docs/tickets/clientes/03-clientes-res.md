# Módulo Clientes: preguntas (grill-me)

**Cómo completarlo:** marquen una opción (o escriban otra) en "Respuesta". ★ = la que recomiendo, con el motivo. **(clienta)** = probablemente solo la sepa ella.

**Ya decidido:** hay un cliente genérico "Consumidor final" (si confirman la pregunta 1 de Ventas); no hay cuenta corriente.

**1. (clienta) ¿Qué tipos de cliente hay?**
- a) ★ Dos, como dice el PDF: gran cliente y consumidor final. _Motivo: alcanza para las estadísticas que pide el PDF._
- b) Más tipos (por ejemplo: comercio, institución, particular).

Respuesta: a

**2. ¿Qué datos son obligatorios para un gran cliente?**
- a) ★ Nombre o razón social y CUIT; el resto opcional. _Motivo: el CUIT lo identifica sin duplicados._
- b) Nombre, CUIT, teléfono y dirección.
- c) Solo el nombre.

Respuesta: Unificar con pregunta 3 y seria la a.

**3. ¿Y para un consumidor final que quiere quedar registrado?**
- a) ★ Nombre y un dato de contacto (teléfono o email); DNI opcional. _Motivo: no frena la carga en el mostrador._
- b) Nombre y DNI obligatorios.
- c) No se registran: todos van al cliente genérico.

Respuesta: Unificar con pregunta 2 y seria la 2a.

**4. (clienta) ¿Qué son las "condiciones comerciales" del PDF?**
- a) ★ Un descuento fijo pactado con ese cliente. _Motivo: coincide con lo recomendado en Ventas pregunta 2._
- b) Plazo de pago (por ejemplo 30 días). _Ojo: contradice "no hay cuenta corriente"._
- c) Un texto libre, solo informativo.

Respuesta:

**5. ¿Un cliente se puede borrar?**
- a) ★ No: se desactiva (deja de aparecer al vender) y conserva su historial. _Motivo: tiene ventas asociadas que no se pueden perder._
- b) Sí, si no tiene ventas.

Respuesta: a

**6. ¿Cómo se evita cargar dos veces el mismo cliente?**
- a) ★ CUIT/DNI único (si está cargado) y aviso si ya existe uno con nombre parecido. _Motivo: evita duplicados sin trabar la carga._
- b) Solo CUIT/DNI único.
- c) No se controla.

Respuesta: a

**7. ¿Quién puede crear y editar clientes?**
- a) ★ Vendedores y administradores (el PDF le da "gestión de clientes" al vendedor). _Motivo: lo dice el PDF._
- b) Solo administradores.

Respuesta: a

---

## Notas del diagrama

- **Cliente.cuit es PK `int`.** Un CUIT de 11 dígitos no entra en `int`, y el consumidor final no tiene CUIT. Propuesta: `id_cliente serial` PK, `cuit_dni varchar(13)` nulo y único.
- Agregar `activo boolean` (pregunta 5) y `descuento_pactado decimal(5,2)` nulo si eligen 4a.
- `tipo_cliente` como enum (gran_cliente, consumidor_final).

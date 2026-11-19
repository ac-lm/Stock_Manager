# Módulo Costos (ingresos y egresos de dinero): preguntas (grill-me)

**Cómo completarlo:** marquen una opción (o escriban otra) en "Respuesta". ★ = la que recomiendo, con el motivo. **(clienta)** = probablemente solo la sepa ella.

**Ya decidido en Caja:** los gastos chicos pagados con plata de la caja son un egreso de caja y pueden vincularse con un costo (`id_costo`). "Ingreso" y "egreso" a secas siempre son dinero.

---

**1. ¿Qué es un "costo" en el sistema?**
- a) ★ Todo gasto del taller que se registra: insumos, sueldos, servicios y otros gastos, con monto, fecha, período y tipo. _Motivo: es la lista del PDF._
- b) Solo los gastos que no son compra de insumos (los insumos se calculan desde los ingresos de mercadería).

Respuesta:

**2. Cuando entra mercadería de un proveedor, ¿se registra también como costo?**
- a) ★ Sí, automáticamente: el ingreso de mercadería genera un costo de tipo "insumos" por el total. _Motivo: no se carga dos veces y el reporte de costos queda completo._
- b) No, se carga el costo a mano aparte.
- c) No: el costo de insumos se calcula desde los ingresos de mercadería en los reportes.

Respuesta:

**3. (clienta) ¿Cómo se pagan los costos?**
- a) Siempre por fuera de la caja (banco, transferencia desde otra cuenta).
- b) ★ Algunos con plata de la caja (gastos chicos) y el resto por fuera. _Motivo: coincide con lo decidido en Caja._
- c) Todos desde la caja.

Respuesta:

**4. ¿Cómo se asocia un costo a un período?**
- a) ★ Con mes y año (por ejemplo, sueldos de octubre 2026), además de la fecha de pago. _Motivo: los reportes del PDF son por período._
- b) Solo con la fecha de pago.
- c) Con un rango de fechas (desde, hasta).

Respuesta:

**5. ¿Los tipos de costo son fijos?**
- a) ★ Sí, un enum: insumos, sueldos, servicios, otros. _Motivo: son los del PDF; "otros" con descripción cubre el resto._
- b) No, el administrador puede crear tipos nuevos (tabla).

Respuesta:

**6. (clienta) Sueldos: ¿se registra por persona o un total?**
- a) ★ Un total por período, con observación. _Motivo: el sistema no es de liquidación de sueldos; evita datos sensibles de cada empleado._
- b) Por persona (haría falta una tabla de empleados, distinta de Usuario).

Respuesta:

**7. "Evolución del costo de los insumos": ¿de dónde sale?**
- a) ★ Del costo unitario de cada ingreso de mercadería: cada compra deja un precio con fecha. _Motivo: no hace falta cargar nada extra._
- b) De una tabla de historial de precios que se actualiza a mano.

Respuesta:

**8. ¿Quién puede ver y cargar costos?**
- a) ★ Solo administradores (el PDF pone "Costos" solo en el perfil Administrador). Los gastos chicos de caja los registra el vendedor como egreso, sin ver el módulo de costos. _Motivo: lo dice el PDF._
- b) Vendedores y administradores.

Respuesta:

---

## Notas del diagrama

- Hoy no hay tabla Costo. Propuesta: `Costo` (id, tipo enum, descripcion, monto decimal(12,2), fecha_pago, periodo_mes, periodo_anio, id_usuario, id_ingreso_mercaderia FK nula, observaciones).
- `Insumo.costo` pasaría a ser el "último costo conocido"; el historial vive en los ingresos.

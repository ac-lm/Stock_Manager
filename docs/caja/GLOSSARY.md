# Glosario: Sistema de Gestión del Taller Protegido de Lobería

Lenguaje común del equipo y la clienta. Cada término tiene un solo significado. Si un término se usa distinto en una charla o en el código, se corrige acá o se corrige el uso.

## Caja

**Caja**
El lugar único donde el taller guarda y cobra el dinero de las ventas. Hay una sola caja.
_Evitar:_ "caja" para referirse a una sesión ("abrí una caja nueva").

**Sesión de caja**
El período entre la apertura y el cierre de la caja. Cuando una sesión está abierta, toda venta y todo egreso se registra en ella. Nunca hay dos sesiones abiertas a la vez. En un mismo día puede haber varias.
_No confundir con:_ Cierre, que es el acto que termina la sesión.

**Apertura**
El acto de iniciar una sesión de caja. Lo hace un usuario y declara el monto inicial.

**Monto inicial**
El dinero que hay en la caja al abrir una sesión. El sistema sugiere el total declarado del cierre anterior. Si quien abre lo cambia, tiene que dar un motivo.

**Cierre**
El acto de terminar una sesión de caja: quien cierra declara cuánto hay por cada medio de pago y el sistema lo compara con lo esperado. Lo puede hacer cualquier usuario con permiso, no necesariamente quien abrió.
_Evitar:_ "cierre" como sinónimo de sesión.

**Movimiento de caja**
Cada entrada o salida de dinero dentro de una sesión. Siempre tiene un medio de pago, un monto y un usuario que lo registró. Un movimiento registrado nunca se borra ni se modifica.

**Ingreso (de dinero)**
Movimiento de caja que suma dinero. Hoy, solo el cobro de una venta.
_No confundir con:_ Ingreso de mercadería. "Ingreso" a secas siempre es dinero.

**Egreso (de dinero)**
Movimiento de caja que resta dinero: un retiro, un gasto chico pagado con dinero de la caja o la devolución por una venta anulada.
_No confundir con:_ Salida de mercadería.

**Retiro**
Egreso en el que alguien saca dinero de la caja sin que sea un gasto, por ejemplo para depositarlo.

**Medio de pago**
La forma en que entra o sale el dinero: efectivo o transferencia.

**Pago mixto**
Una venta cobrada con más de un medio de pago, por ejemplo una parte en efectivo y otra por transferencia. Genera un ingreso por cada medio.

**Total esperado**
Lo que debería haber al cerrar, por cada medio de pago: monto inicial (solo efectivo) más ingresos menos egresos de la sesión.

**Total declarado**
Lo que la persona que cierra cuenta o verifica, por cada medio de pago. El efectivo se cuenta y no puede ser negativo; las transferencias se controlan contra el banco y se declaran como neto de la sesión (cobros menos pagos), que puede ser negativo si los egresos superan a los ingresos.

**Diferencia**
Total declarado menos total esperado. Negativa es faltante, positiva es sobrante.

**Pendiente de revisión**
Estado de una sesión cerrada con diferencia distinta de cero. Queda así hasta que un administrador la revisa y deja su observación.

**Corrección de cierre**
Cambio que hace un administrador sobre un cierre ya registrado. Siempre deja constancia de quién lo hizo, cuándo, y el valor anterior y el nuevo.

## Ventas

**Venta**
La entrega de productos a un cliente a cambio de dinero. Se cobra completa en el momento de confirmarla: no hay ventas a pagar después. No se puede confirmar una venta sin una sesión de caja abierta. Toda venta tiene un cliente, aunque sea el cliente genérico. No existen ventas a medio cargar: una venta existe recién cuando se confirma.
_Evitar:_ "venta pendiente" o "borrador de venta". Lo que queda pendiente es un pedido.

**Línea de venta**
Cada producto dentro de una venta, con su cantidad, su precio de catálogo al momento de vender y su descuento.
_Evitar:_ "ítem" o "renglón" como términos distintos.

**Precio de catálogo**
El precio que tiene un producto en el catálogo en el momento de la venta. Al vender no se cambia: para cobrar menos se aplica un descuento. Una venta ya hecha conserva el precio con el que se vendió aunque después cambie el catálogo.
_Evitar:_ "precio de lista" como sinónimo.

**Descuento**
Rebaja expresada en porcentaje. Cualquier usuario puede aplicarlo. Falta que la clienta confirme si se aplica sobre cada línea de venta, sobre el total, o en los dos lugares.
_Evitar:_ cambiar el precio como forma de rebajar.

**Remito**
Comprobante de entrega de los productos de una venta. Se genera automáticamente con cada venta.
_No confundir con:_ factura.

**Venta anulada**
Una venta confirmada que se deja sin efecto completa. La venta no se borra: cambia su estado y se registra un egreso por el mismo monto en la sesión abierta en ese momento. No existe la anulación parcial: si el cliente devuelve parte de lo comprado, se anula la venta entera y se registra una venta nueva con lo que se queda.
_Evitar:_ "devolución" como concepto propio.

## Clientes

**Cliente**
Persona, comercio o institución a la que el taller le vende. Se identifica por su CUIT o DNI, que no se puede repetir. Un cliente no se borra: se desactiva.

**Gran cliente**
Tipo de cliente que compra en volumen o de forma habitual, normalmente un comercio o institución, y queda registrado con sus datos.

**Consumidor final**
Tipo de cliente particular que compra para sí mismo. Puede quedar registrado con sus datos si quiere.
_No confundir con:_ Cliente genérico, aunque este se llame "Consumidor final".

**Cliente genérico**
El único cliente sin CUIT ni DNI, llamado "Consumidor final", al que se asignan las ventas de mostrador cuando el comprador no se registra. No se puede editar ni desactivar.
_Evitar:_ "venta sin cliente".

**Cliente inactivo**
Cliente que ya no aparece para elegir al vender, pero conserva todo su historial. Se puede volver a activar.
_Evitar:_ "cliente borrado" o "dado de baja" como si se perdieran sus datos.

## Stock

**Ingreso de mercadería**
Entrada de insumos o productos al stock, de un proveedor u otra fuente. No mueve dinero de la caja.
_Evitar:_ "ingreso" a secas para hablar de mercadería.

**Salida de mercadería**
Salida de insumos o productos del stock por un motivo distinto de la venta: descarte, pérdida, daño o ajuste.
_Evitar:_ "salida" o "egreso" para hablar de dinero.

## Usuarios

**Administrador**
Usuario con acceso a todo el sistema. Es el único que puede revisar sesiones pendientes y corregir cierres.

**Vendedor**
Usuario que registra ventas, gestiona clientes y opera la caja (abre, cobra, registra egresos y cierra). Lo que la clienta llamó "empleado" es un vendedor.
_Evitar:_ "empleado" como rol del sistema.

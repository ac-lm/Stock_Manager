# Módulo Usuarios y auditoría: preguntas (grill-me)

**Cómo completarlo:** marquen una opción (o escriban otra) en "Respuesta". ★ = la que recomiendo, con el motivo. **(clienta)** = probablemente solo la sepa ella.

**Ya decidido:** dos roles, Administrador y Vendedor ("empleado" = vendedor); Rol es un enum; login con JWT.

---

## A. Usuarios

**1. ¿Con qué se loguea un usuario?**
- a) ★ Nombre de usuario y contraseña. _Motivo: puede haber personas del taller sin email propio._


**2. ¿Quién crea los usuarios?**
- a) ★ Solo el administrador; no hay registro libre. _Motivo: es un sistema interno; el PDF pone "gestión de usuarios" en el Administrador._


**3. ¿Cómo se recupera una contraseña olvidada?**

- b) * Link por email.


**4. ¿Un usuario se puede borrar?**
- a) ★ No: se desactiva y no puede entrar, pero quedan sus ventas, cierres y auditoría. Motivo: tiene operaciones a su nombre.


**5. ¿Cuánto dura la sesión?**
- a) ★ 8 horas (un turno); después pide login de nuevo. _Motivo: equilibrio entre comodidad y seguridad en una compu compartida._


PREGUNTAR **6. (clienta) ¿Varias personas usan la misma compu del mostrador?**
- a) ★ Sí: hay que cerrar sesión al terminar el turno; el sistema lo recuerda al cerrar la caja. _Motivo: si no, las ventas quedan a nombre de otro._
- b) No, cada uno tiene la suya.


## B. Auditoría

**7. ¿Qué operaciones se auditan?**
- a) ★ Las "relevantes": altas, cambios y bajas de cualquier registro, más login, apertura, cierre y corrección de caja, anulación de ventas y ajustes de stock. No se auditan las consultas. _Motivo: es lo que pide el PDF sin llenar la tabla de ruido._


**8. ¿Cómo se guarda "la modificación realizada"?**
- a) ★ Valor anterior y valor nuevo de los campos que cambiaron, en una columna JSON. _Motivo: sirve para cualquier tabla con una sola tabla de auditoría._


**9. ¿Cómo se registra la auditoría en el código?**
- a) ★ Automáticamente, con Hibernate Envers o un listener de JPA, más llamadas explícitas para acciones de negocio (cerrar caja, anular venta). _Motivo: nadie se olvida de auditar un cambio._


**10. ¿Quién ve la auditoría?**
- a) ★ Solo administradores, con filtros por usuario, fecha, tabla y acción. _Motivo: es información de control._


---

## Notas del diagrama

- **Auditoria tiene casi todo en `int`:** `hora`, `accion_realizada`, `registro_afectado` y `modificacion_realizada`. Propuesta: `fecha_hora timestamp`, `accion` enum (alta, modificacion, baja, login, …), `tabla varchar(50)`, `id_registro bigint`, `cambios jsonb` (o `text`).
- `Usuario.passwor`: el nombre quedó cortado (`password_hash`). `varchar(150)` está bien para un hash BCrypt (60 caracteres).
- Agregar `username` único, `activo`, `debe_cambiar_password`.
- `rol varchar(30)` pasa a enum.
- `jsonb` existe igual en Postgres 15 y 17.

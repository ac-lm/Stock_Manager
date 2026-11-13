# Modelo de base de datos

El modelo relacional del proyecto se diseña en [Vertabelo](https://www.vertabelo.com/) y ese
diseño es la **fuente de verdad** de la base de datos.

## Qué guardar en esta carpeta

El XML exportado por Vertabelo debe versionarse acá con el nombre `modelo-vertabelo.xml`:

```
docs/db/
├── README.md              (este archivo)
└── modelo-vertabelo.xml   (export de Vertabelo)
```

Para exportarlo:

1. Abrir el modelo en Vertabelo.
2. Menú **File → Export → XML**.
3. Guardar el archivo como `docs/db/modelo-vertabelo.xml`.

## Reglas

- Cualquier cambio de tablas, columnas, tipos o claves debe hacerse **primero en Vertabelo** y
  después en las entidades JPA del backend.
- Al cambiar el modelo, exportar el XML de nuevo y commitearlo junto con los cambios de código.
- No editar el XML a mano: se regenera desde Vertabelo.
- Los nombres de tablas y columnas del XML son los que se usan en `@Table` y `@Column`.

## Situación actual

El XML todavía **no está versionado** en esta carpeta. Mientras tanto, el detalle de las tablas
implementadas está en [`../modelo-datos.md`](../modelo-datos.md).

> Pendiente: agregar `modelo-vertabelo.xml` con el export completo (22 tablas).

# ADR-0006 – Organización del código por funcionalidad bajo el paquete `app`

- **Estado:** Aceptada
- **Fecha:** 2026-10-05
- **Etapa:** 1
- **Origen:** decisión del equipo

## Contexto
La consigna evalúa la mantenibilidad, el bajo acoplamiento y la alta cohesión, y anticipa funcionalidades nuevas en las Etapas 2 y 3. La estructura de paquetes condiciona cuánto se modifica lo existente al agregar algo nuevo.

## Decisión
- Paquete raíz **`app`**, que mantiene la estructura simple del proyecto de referencia.
- Subpaquetes **por funcionalidad**, y dentro de cada uno, las capas:

```
app/
├── LinkShortenerApplication.java
├── link/      entidad, service, controllers, dto/, alias/
├── qr/        generación del código QR
└── common/    config/ y error/ (transversales)
```

- Una funcionalidad nueva se agrega como paquete hermano (por ejemplo `app.stats`).
- Dependencias permitidas: los controllers usan services y DTOs, y los services usan entidades y `EntityManager`. Nada depende de los controllers.

## Alternativas consideradas
- **Por capa** (`app.controller`, `app.service`, `app.entity`): es más común, pero cada funcionalidad nueva obliga a tocar todos los paquetes y las clases relacionadas quedan dispersas.
- **Arquitectura hexagonal completa:** demasiado pesada para el alcance actual.

## Consecuencias
- Lo que cambia junto queda junto, lo que da alta cohesión.
- Los cambios de etapas futuras tienden a quedar dentro de un paquete, lo que da bajo acoplamiento.
- Hay que cuidar que `common` no se convierta en un cajón de sastre: ahí solo va lo realmente transversal.

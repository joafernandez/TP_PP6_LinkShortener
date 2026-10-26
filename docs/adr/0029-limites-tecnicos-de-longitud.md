# ADR-0029 – Límites técnicos de longitud del espacio y del código personalizado

- **Estado:** Aceptada
- **Fecha:** 2026-10-25
- **Etapa:** 2
- **Origen:** **decisión técnica del equipo**. No es un requerimiento del Cliente.

## Contexto
El Cliente **no definió** un largo mínimo ni máximo para el espacio de nombres ni para el código personalizado. Igual hace falta un máximo técnico: la base de datos necesita un tamaño de columna, y una URL muy larga deja de ser práctica para un acortador.

## Decisión
| Elemento | Mínimo | Máximo | Se aplica en |
|---|---|---|---|
| Espacio de nombres | Sin mínimo (1 carácter es válido) | **15 caracteres** | Paso 2.1: columna `namespace.name VARCHAR(15)` |
| Código personalizado | Sin mínimo | **15 caracteres** | Paso 2.4 |

Además, en el paso 2.3 se impedirá usar como espacio los nombres que chocan con rutas del sistema (`api`, `assets`, `v3`, `error`, `login`, `logout`, `oauth2`), con una lista configurable. También es una decisión técnica: un espacio `v3` haría que `/v3/api-docs` dejara de funcionar.

Los caracteres permitidos **sí** son criterio del equipo aprobado sobre el requerimiento del Cliente ([ADR-0028](0028-modelo-de-usuarios-y-espacios.md)): minúsculas y números para el espacio; minúsculas, números, `-` y `_` para el código personalizado.

## Alternativas consideradas
- **Sin máximo:** la columna necesita un tamaño, y textos sin límite generan URLs poco prácticas.
- **Máximos mayores (30 y 64):** fue la primera propuesta; el equipo prefirió 15 para mantener las URLs cortas.
- **Mínimo de 3 caracteres:** no hay motivo técnico para exigirlo y el Cliente no lo pidió.

## Consecuencias
- Si el Cliente define otros límites, alcanza con una migración nueva y un cambio de validación, sin cambiar el modelo.
- Los mensajes de error deben presentar el límite como regla del sistema, sin atribuirlo al Cliente.

# ADR-0016 – Formato del alias: alfanumérico aleatorio de 5 caracteres

- **Estado:** Aceptada (supuesto del equipo, pendiente de confirmación del Cliente – D1)
- **Fecha:** 2026-10-05
- **Etapa:** 1
- **Origen:** decisión del equipo ante una ambigüedad de la consigna ([minuta 01](../etapa-1/minutas/minuta-01-elucidacion.md), D1)

## Contexto
La consigna exige un alias único y *"tan corto y memorizable como sea posible"*, con ejemplos alfanuméricos (`xT3se`) y numéricos (`15321`). Los enlaces suelen apuntar a recursos privados, como carpetas compartidas de Google Drive.

## Decisión
- Alias **aleatorio**, de **5 caracteres**. El largo es configurable.
- Alfabeto de 56 caracteres, **sin caracteres ambiguos** (se excluyen `0`, `O`, `o`, `1`, `I` y `l`):
  `23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz`
- Distingue mayúsculas de minúsculas.
- Nunca se genera una palabra reservada (ver [ADR-0011](0011-ruta-del-alias-y-palabras-reservadas.md)).
- Si el alias generado ya está en uso por un enlace vigente, se genera otro, con un máximo de intentos configurable.
- Se implementa detrás de la interfaz `AliasGenerator` (ver [ADR-0010](0010-puntos-de-extension.md)).

## Alternativas consideradas
- **Numérico secuencial:** permite recorrer los enlaces de otros probando números consecutivos.
- **Numérico aleatorio:** con 5 dígitos da 100.000 combinaciones, muchas menos que el alfanumérico con el mismo largo.
- **Alfanumérico completo (62 caracteres):** incluye caracteres que se confunden al leer o dictar.

## Consecuencias
- 56⁵ ≈ 550 millones de combinaciones. Con una vigencia de 60 minutos, la cantidad de enlaces activos es muy baja en comparación, así que las colisiones son raras.
- Cambiar el formato o el largo implica otra implementación de `AliasGenerator` o un cambio de configuración, sin tocar el service.

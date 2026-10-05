# ADR-0019 – Se rechaza acortar URLs del propio servicio

- **Estado:** Aceptada (supuesto del equipo, pendiente de confirmación del Cliente – D4)
- **Fecha:** 2026-10-05
- **Etapa:** 1
- **Origen:** decisión del equipo ante una ambigüedad de la consigna ([minuta 01](../etapa-1/minutas/minuta-01-elucidacion.md), D4)

## Contexto
Si se acorta una URL corta del mismo servicio, se arman cadenas de redirección. Incluso podría generarse un bucle: un enlace que, tras una reasignación, termina apuntando a sí mismo.

## Decisión
Se rechaza con **HTTP 400** (ProblemDetail) toda URL cuyo esquema, host y puerto coincidan con la URL base configurada del servicio (`app.base-url`).

## Alternativas consideradas
- **Aceptarlas:** no aportan nada y habilitan cadenas y bucles de redirección.

## Consecuencias
- Se eliminan los bucles de redirección dentro del propio servicio.
- La regla depende de que `app.base-url` esté bien configurada ([ADR-0022](0022-dominio-configurable.md)).

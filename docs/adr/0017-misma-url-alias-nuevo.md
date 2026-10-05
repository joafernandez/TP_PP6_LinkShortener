# ADR-0017 – Acortar una URL ya acortada genera un alias nuevo

- **Estado:** Aceptada (supuesto del equipo, pendiente de confirmación del Cliente – D2)
- **Fecha:** 2026-10-05
- **Etapa:** 1
- **Origen:** decisión del equipo ante una ambigüedad de la consigna ([minuta 01](../etapa-1/minutas/minuta-01-elucidacion.md), D2)

## Contexto
La consigna no indica qué ocurre si se acorta una URL que ya tiene un enlace vigente.

## Decisión
Cada pedido de acortamiento genera un **alias nuevo**, con su propia vigencia de 60 minutos. No se buscan duplicados ni se renuevan enlaces existentes.

## Alternativas consideradas
- **Devolver el alias vigente:** quien lo recibe podría quedarse con solo unos minutos de vigencia, según cuándo lo pidió otra persona.
- **Devolver el alias vigente y renovar su vigencia:** el pedido de un usuario alteraría el enlace de otro.

## Consecuencias
- Cada enlace es independiente y su vigencia es siempre de 60 minutos desde que se pidió, como indica la consigna.
- Una misma URL puede tener varios alias vigentes al mismo tiempo.
- La creación es más simple: no hay que consultar duplicados.

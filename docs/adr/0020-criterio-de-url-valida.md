# ADR-0020 – Criterio de URL válida

- **Estado:** Aceptada (supuesto del equipo, pendiente de confirmación del Cliente – D5)
- **Fecha:** 2026-10-05
- **Etapa:** 1
- **Origen:** decisión del equipo ante una ambigüedad de la consigna ([minuta 01](../etapa-1/minutas/minuta-01-elucidacion.md), D5)

## Contexto
La consigna exige una *"URL original válida"* sin definir el criterio, y menciona el acceso *"vía HTTP/HTTPS"*.

## Decisión
Una URL es válida si cumple todo lo siguiente:
- No está vacía.
- Tiene como máximo **2048 caracteres**.
- Es una **URL absoluta** con esquema **`http` o `https`**. Se acepta en mayúsculas o minúsculas.
- Tiene **host**.
- No pertenece al propio servicio ([ADR-0019](0019-rechazo-de-urls-del-propio-servicio.md)).

**No se verifica** que la URL exista o responda. Si no es válida, se responde **HTTP 400** con ProblemDetail.

## Alternativas consideradas
- **Aceptar cualquier esquema:** habilita usos maliciosos (`javascript:`, `data:`, `file:`).
- **Verificar que la URL responda:** hace la creación lenta y dependiente de terceros, y falla con recursos privados que sí existen (por ejemplo, carpetas de Drive que requieren sesión).

## Consecuencias
- La validación es rápida, determinística y fácil de testear.
- Se puede acortar una URL bien formada que no exista. Es aceptable para la Etapa 1.

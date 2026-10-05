# ADR-0018 – Enlace vencido o inexistente: página HTML con código 404

- **Estado:** Aceptada (supuesto del equipo, pendiente de confirmación del Cliente – D3)
- **Fecha:** 2026-10-05
- **Etapa:** 1
- **Origen:** decisión del equipo ante una ambigüedad de la consigna ([minuta 01](../etapa-1/minutas/minuta-01-elucidacion.md), D3)

## Contexto
La consigna establece que, pasados 60 minutos, el enlace *"dejará de redireccionar"*. No indica qué ve el usuario. Quien accede a la URL corta es una persona, desde el navegador o al escanear el QR.

## Decisión
- Al acceder a `/{alias}` con un alias **vencido o inexistente**, se responde **HTTP 404** con una **página HTML simple** que indica que el enlace no está disponible o venció.
- No se distingue entre vencido e inexistente.

## Alternativas consideradas
- **Respuesta JSON o sin cuerpo:** no le sirve a una persona que usa el navegador.
- **410 Gone para los vencidos:** indica una eliminación permanente, y el alias puede reasignarse.
- **Redirigir a la página principal:** confunde, porque el usuario no sabe qué pasó con el enlace.
- **Distinguir vencido de inexistente:** sin historial ([ADR-0021](0021-sin-historial-de-enlaces.md)) no siempre es posible, y además revela información.

## Consecuencias
- Comportamiento claro y uniforme para el usuario final.
- Los errores de la API (`/api/v1/...`) siguen usando ProblemDetail en JSON ([ADR-0007](0007-diseno-de-la-api-rest.md)). Solo la redirección responde con HTML.

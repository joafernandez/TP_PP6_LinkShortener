# ADR-0012 – Redirección con HTTP 302

- **Estado:** Aceptada
- **Fecha:** 2026-10-05
- **Etapa:** 1
- **Origen:** consigna (redirección transparente, vigencia de 60 minutos y reasignación de alias)

## Contexto
Al acceder a la URL corta, el sistema debe redirigir a la URL original. Pero el enlace vence a los 60 minutos y su alias puede reasignarse a otra URL.

## Decisión
La redirección responde **`302 Found`** con el encabezado `Location` apuntando a la URL original.

## Alternativas consideradas
- **`301 Moved Permanently`:** el navegador lo guarda en caché por tiempo indefinido. Después de vencido o reasignado el alias, ese navegador seguiría yendo a la URL vieja sin consultar al servidor, lo que viola la regla de vencimiento.
- **`307 Temporary Redirect`:** también es temporal, pero conserva el método HTTP. Para un acceso con GET no hay diferencia práctica, y el 302 es el más compatible.

## Consecuencias
- Cada acceso pasa por el servidor, que siempre puede aplicar la regla de vencimiento.
- No se aprovecha la caché del navegador. Es aceptable para el volumen previsto.
- Lo que se responde ante un enlace vencido o inexistente es parte de D3 y está pendiente de respuesta del Cliente.

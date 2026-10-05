# ADR-0011 – Ruta `/{alias}` alfanumérica y lista de palabras reservadas

- **Estado:** Aceptada
- **Fecha:** 2026-10-05
- **Etapa:** 1
- **Origen:** consigna (formato `http://{dominio_o_ip}/{alias}`) y decisión del equipo

## Contexto
La consigna fija que el alias va en la **raíz** de la URL. En esa misma raíz se sirven la página web (`/index.html`, `/css/...`), Swagger UI (`/swagger-ui/...`, `/v3/api-docs`), la API (`/api/...`) y la ruta de error de Spring (`/error`). Una ruta `/{alias}` sin restricciones capturaría esas direcciones, y un alias que coincida con una ruta fija quedaría inaccesible.

## Decisión
- La redirección se mapea como **`/{alias:[A-Za-z0-9]+}`**, es decir, un solo segmento alfanumérico. Así no captura rutas con punto, guion o varios segmentos.
- Una **lista configurable de palabras reservadas** (inicialmente `api`, `error`, `v3`, `swagger`) que el generador de alias nunca produce.

## Alternativas consideradas
- **Prefijo para los alias** (por ejemplo `/r/{alias}`): elimina los conflictos, pero contradice el formato que pide la consigna y alarga la URL.
- **Solo la expresión regular, sin palabras reservadas:** no cubre las rutas fijas de un solo segmento alfanumérico, como `api` o `error`.

## Consecuencias
- Cumple el formato de la consigna sin conflictos con los recursos estáticos ni con la API.
- No adelanta la decisión D1: los dos formatos de ejemplo de la consigna (`xT3se` y `15321`) son alfanuméricos.
- Si se agrega una ruta fija nueva de un solo segmento, hay que sumarla a las palabras reservadas.

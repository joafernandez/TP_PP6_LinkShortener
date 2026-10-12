# 012 – Incremento 7: extensión para Chrome y Firefox

- **Fecha:** 2026-10-11
- **Herramienta:** Claude (claude.ai)
- **Objetivo:** implementar la extensión que acorta la página actual con un clic y muestra la URL corta y el QR (HU-02, HU-03 y HU-06), reutilizando el backend existente.
- **Estado:** implementado y revisado de forma estática; **pendiente de la prueba manual en Chrome y Firefox** (ver `extension/README.md`).

## Pedido del equipo

El integrante responsable de los incrementos 7 y 8 pidió analizar el repositorio (zip), confirmar si era posible hacer el incremento 7 y, tras ver el análisis y la lista de archivos, aprobó implementarlo. Restricciones indicadas: **no usar Node** en el proyecto (por eso no hay tests automáticos de la extensión) y explicar en pasos pequeños con equivalencias con PHP.

## Diseño antes del código

- Aplicar ADR-0014: Manifest V3 único, popup con `activeTab`, pantalla de opciones y `optional_host_permissions`, sin CORS.
- El popup, al abrirse, lee la URL de la pestaña activa y la envía a `POST /api/v1/links`. Muestra `shortUrl`, `expiresAt` en hora local y el QR con un `<img>` que apunta a `GET /api/v1/links/{alias}/qr` (ADR-0023). No agrega endpoints ni reglas: el cliente no fija el TTL ni genera alias.
- La extensión solo comprueba que la pestaña sea `http(s)` para dar un mensaje claro en páginas como `chrome://` o `about:` (CA-02.2); la validación real queda en el backend.
- Casos de error cubiertos: página no acortable, sin configurar, sin permiso, servidor inaccesible y errores ProblemDetail (400, 503).
- Opciones: se valida y normaliza la dirección (esquema, host y puerto); `permissions.request()` se llama en el manejador del envío, antes de cualquier `await`; la dirección se guarda en `storage.local` solo si se acepta el permiso.
- Dos datos técnicos contrastados con la documentación de MDN: Firefox no admite puertos en los patrones de host, así que el permiso se pide por host sin puerto; y `optional_host_permissions` está disponible desde Firefox 128, por eso `strict_min_version` es 128.0.
- Sin dependencias nuevas, sin build y sin cambios en el backend.

## Resultados

- Archivos nuevos en `extension/`: `manifest.json`, `popup/` (HTML, CSS, JS), `options/` (HTML, CSS, JS), `src/config.js`, `src/api.js`, `icons/` (16, 32, 48 y 128 px) y `README.md` con la guía de carga y 12 casos de prueba manual mapeados a los criterios de aceptación.
- Revisión estática hecha: el manifest es JSON válido y todas las rutas existen; los `id` usados por cada JS existen en su HTML; los imports resuelven a exports existentes.
- **No se ejecutó nada**: no se cargó la extensión en un navegador real, no se corrió el backend ni sus tests (el entorno de esta sesión no tiene JDK 25 ni red) y no se ejecutó el JavaScript. Por eso el incremento no se da por cerrado hasta completar la prueba manual.
- Decisión propuesta, pendiente de aprobación: registrar un ADR-0024 con la elección de pedir el permiso de host sin puerto.

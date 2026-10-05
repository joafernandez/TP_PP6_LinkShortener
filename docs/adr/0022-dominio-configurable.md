# ADR-0022 – Dominio o IP configurable en el servidor y en la extensión

- **Estado:** Aceptada (supuesto del equipo, pendiente de confirmación del Cliente – D7)
- **Fecha:** 2026-10-05
- **Etapa:** 1
- **Origen:** decisión del equipo ante una ambigüedad de la consigna ([minuta 01](../etapa-1/minutas/minuta-01-elucidacion.md), D7)

## Contexto
La consigna define el formato `http://{dominio_o_ip}/{alias}`, con el ejemplo `http://192.168.1.50/xT3se`. La IP depende de la red donde se ejecute el servicio.

## Decisión
- La URL base del servicio se define con la propiedad **`app.base-url`**, que se puede sobrescribir con una variable de entorno. La URL corta se arma como `{app.base-url}/{alias}`.
- La extensión tiene una **pantalla de opciones** para configurar la dirección del backend ([ADR-0014](0014-extension-mv3-y-permisos-de-host.md)).
- Por defecto se usa HTTP. Si el Cliente requiere HTTPS, se resolverá en un ADR nuevo.

## Alternativas consideradas
- **Dominio fijo en el código:** obliga a recompilar en cada red.
- **Deducirlo de cada pedido HTTP:** depende de cómo acceda cada cliente y puede generar URLs cortas inconsistentes.

## Consecuencias
- El mismo artefacto funciona en cualquier red: solo cambia la configuración.
- Queda pendiente saber en qué entorno se hará la demostración y si se requiere HTTPS.

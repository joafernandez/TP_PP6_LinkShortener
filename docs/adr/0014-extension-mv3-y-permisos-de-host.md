# ADR-0014 – Extensión MV3 única para Chrome y Firefox, con permisos de host en lugar de CORS

- **Estado:** Aceptada
- **Fecha:** 2026-10-05
- **Etapa:** 1
- **Origen:** consigna (complemento para Chrome y Firefox) y decisión del equipo

## Contexto
La consigna pide un complemento para Chrome y Firefox que, con un solo clic, genere y muestre la URL corta y el QR de la página actual. La extensión tiene que llamar al backend, cuya dirección no es fija porque depende de la red (ver D7).

## Decisión
- **Manifest V3**, con un único código para los dos navegadores (en Firefox se agrega `browser_specific_settings.gecko.id`).
- Un **popup** que al abrirse lee la URL de la pestaña activa (permiso `activeTab`), llama a la API y muestra el resultado.
- Una **pantalla de opciones** para configurar la dirección del backend.
- **Sin CORS.** El acceso al backend se habilita con **`optional_host_permissions`**: la pantalla de opciones pide el permiso para el host configurado con `permissions.request()`.
- `permissions.request()` se llama **directamente en el handler del clic de Guardar, antes de cualquier `await`**.

## Alternativas consideradas
- **CORS que permita solo el origen de la extensión:** no funciona en Firefox, donde el origen (`moz-extension://<UUID>`) es distinto en cada instalación.
- **`host_permissions` fijos:** la dirección del backend no se conoce de antemano. Abrir `http://*/*` da más permisos de los necesarios.
- **CORS abierto (`*`) en la API:** expone la API a cualquier sitio web sin necesidad.

## Consecuencias
- Funciona igual en Chrome y Firefox, y solo se pide permiso para el host que se usa.
- Si `permissions.request()` se llama después de un `await`, el navegador (sobre todo Firefox) lo rechaza porque deja de considerarlo una acción del usuario.
- En Firefox MV3 los permisos de host pueden no estar otorgados de entrada. Pedirlos desde la pantalla de opciones resuelve ese caso.
- La extensión no tiene lógica de negocio: solo consume la misma API que la página web.

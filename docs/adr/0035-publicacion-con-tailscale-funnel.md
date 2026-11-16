# ADR-0035 – Publicación con HTTPS mediante Tailscale Funnel

- **Estado:** Aceptada
- **Fecha:** 2026-11-15
- **Etapa:** 2
- **Origen:** decisión del equipo ante la duda E9 de la [minuta 02](../etapa-2/minutas/minuta-02-elucidacion.md) (ver [prompt 010](../etapa-2/prompts/010-publicacion-con-tailscale-funnel.md))

## Contexto
Google solo acepta volver a direcciones con HTTPS o a `http://localhost`. Con `localhost` el celular no llega al servidor, así que el login con QR ([ADR-0033](0033-login-con-qr.md)) no se puede probar ni mostrar desde un celular real. Además, cualquier persona debe poder entrar desde Internet e iniciar sesión con su cuenta de Google, sin estar autorizada de antemano.

Restricciones del equipo: sin servicios pagos, sin comprar dominios, sin exponer credenciales ni la base de datos y con los cambios mínimos en el proyecto.

## Decisión
- La aplicación se publica con **Tailscale Funnel** desde la computadora que la ejecuta: `tailscale funnel --bg 8081`. Funnel da una URL pública fija del estilo `https://<máquina>.<red>.ts.net`, con certificado válido, y reenvía cada pedido a `http://127.0.0.1:8081`.
- `APP_BASE_URL` se configura con esa URL, así que los enlaces cortos y el QR del login apuntan a la dirección pública. No hace falta tocar código: ya se leía de esa variable ([ADR-0022](0022-dominio-configurable.md)).
- Se agrega `server.forward-headers-strategy=native`. Funnel termina el HTTPS y reenvía en HTTP; sin esta propiedad, Spring arma la dirección de vuelta para Google (`/login/oauth2/code/google`) con `http://` y Google la rechaza. Con `native`, Tomcat toma el esquema y el host de `X-Forwarded-Proto` y `X-Forwarded-Host`, pero **solo si el pedido llega desde un proxy interno** (como 127.0.0.1). Sin proxy, el comportamiento no cambia.
- En Google Cloud (lo configura el equipo a mano): dominio autorizado de la red `ts.net`, URI de redireccionamiento `https://<máquina>.<red>.ts.net/login/oauth2/code/google` (se conserva la de `localhost:8081`) y la app en estado **En producción** para que entre cualquier cuenta. Para publicarla, Google exige una política de privacidad en el mismo dominio: se sirve como página estática en `/privacidad.html`.

## Alternativas consideradas
- **Servidor con dominio propio:** requiere pagar el dominio y el alojamiento.
- **ngrok o Cloudflare Tunnel sin cuenta:** el plan gratuito de ngrok da una URL que puede cambiar, y el túnel rápido de Cloudflare cambia la URL en cada arranque. Cada cambio obligaría a reconfigurar Google Cloud.
- **`server.forward-headers-strategy=framework`:** también funciona, pero confía en los encabezados de cualquier origen; alguien de la misma red que llegue directo al puerto 8081 podría falsificarlos.

## Consecuencias
- La URL pública funciona solo mientras la computadora está encendida, con Tailscale activo y la aplicación levantada.
- La URL depende de la máquina: si la demo se hace en otra computadora, hay que repetir Funnel ahí y agregar su URI en Google Cloud (se pueden tener varias a la vez).
- La base HSQLDB no se expone: funciona en modo archivo, sin puerto de red. Sí quedan públicas todas las rutas HTTP de la app, incluida Swagger UI.

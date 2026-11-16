# 010 – Publicación con HTTPS mediante Tailscale Funnel

- **Fecha:** 2026-11-15
- **Herramienta:** Claude (app de escritorio, con la carpeta del proyecto conectada)
- **Objetivo:** obtener una URL pública con HTTPS para usar el login con Google y el login con QR desde el celular (duda E9).
- **Estado:** configuración implementada en la rama `feature/etapa2-tailscale-funnel`, sin commit. Falta configurar Google Cloud con la URL real y probar desde el celular.

## Prompt (textual)

> Quiero publicar mi proyecto *Link Shortener* utilizando *Tailscale Funnel*, para obtener una URL pública con HTTPS, gratuita y accesible desde cualquier computadora o celular.
>
> **Lo que necesito**
> 1. Configurar Tailscale Funnel para publicar mi aplicación Spring Boot, que actualmente funciona en localhost:8081.
> 2. Obtener una URL pública HTTPS que pueda utilizar posteriormente en Google Cloud.
> 3. Configurar el proyecto para que los enlaces cortos se generen con esa URL pública en lugar de localhost.
> 4. Asegurar que la aplicación funcione correctamente desde el celular, incluyendo las sesiones y el acceso a las páginas necesarias para el login mediante QR.
> 5. Mantener HSQLDB y todas las funcionalidades actuales sin modificaciones innecesarias.
>
> **IMPORTANTE**
> - Google Cloud lo voy a configurar yo manualmente. No necesito que hagas esa parte.
> - No modifiques ni rompas las funcionalidades existentes.
> - No agregues servicios pagos ni compres dominios.
> - No expongas credenciales ni la base de datos.
> - Respetá las instrucciones de CLAUDE.md.
> - Realizá únicamente los cambios indispensables.
> - Guiame si necesito instalar Tailscale o autorizar alguna configuración.
> - No hagas commit, push ni merge sin autorización.
>
> **Objetivo:** conseguir una URL pública HTTPS funcional para Link Shortener. Después utilizaré esa dirección para configurar Google Cloud y probar el inicio de sesión desde el celular.
>
> Primero revisá la configuración existente, comprobá que Tailscale Funnel sea compatible y avanzá con la solución más sencilla posible.

## Análisis

- Los enlaces cortos (`EnlaceService`, `EnlaceDeEspacioService`) y la URL del QR del login (`QrLoginService`) ya se arman con `app.base-url`, que se lee de `APP_BASE_URL`: alcanza con configurar la variable.
- Funnel termina el HTTPS y reenvía en HTTP a `127.0.0.1:8081`. Sin leer los encabezados reenviados, Spring Security arma la URI de vuelta para Google con `http://` y el login falla.
- La web usa rutas relativas y HSQLDB está en modo archivo: no hay que tocar el frontend, la extensión ni la base.

## Decisiones

- `server.forward-headers-strategy=native` en `application.properties` (única línea de código). Se eligió `native` en lugar de `framework` porque solo confía en los encabezados que llegan desde un proxy interno. Ver [ADR-0035](../../adr/0035-publicacion-con-tailscale-funnel.md).
- Funnel en la computadora de la demo, con `--bg` para que siga activo tras reiniciar.
- Google Cloud queda a cargo del equipo (dominio autorizado, URI de redireccionamiento y pasar la app a producción).
- Para pasar la app a **En producción**, Google exige una URL de política de privacidad en el mismo dominio: se agrega la página estática `frontend/privacidad.html` (sin cambios de código ni de base).

## Cambios

- `backend/src/main/resources/application.properties`: la propiedad anterior, con comentario.
- `docs/adr/0035-publicacion-con-tailscale-funnel.md` y su fila en `docs/adr/README.md`.
- `README.md`: sección «Publicar con HTTPS (Tailscale Funnel)».
- `frontend/privacidad.html`: política de privacidad que pide Google para publicar la app, y su nombre en la lista de páginas que `backend/build.gradle` copia al JAR.
- `CLAUDE.md`: fila de la tabla 9.1, número del próximo prompt y pendiente de la E9.

## Verificación

- Pendiente: `gradlew test` y la prueba real (login con Google y con QR desde el celular por la URL de Funnel).

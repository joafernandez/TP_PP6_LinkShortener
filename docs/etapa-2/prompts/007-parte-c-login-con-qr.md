# Prompt 007: Parte C, login con QR

**Fecha:** 2026-10-25 · **Rama:** `etapa-2-parte-C`

## Pedido

Implementar el login con QR: la computadora muestra un QR de un solo uso que vence a los 2 minutos; el celular lo escanea, inicia sesión con Google y ve "¿Autorizar el inicio de sesión en la computadora?" (Autorizar / Rechazar); la computadora consulta cada 2 segundos y muestra mensajes claros si fue rechazado, venció, se usó o falló. Contrato OpenAPI primero, tests de aprobado, rechazado, vencido y usado dos veces, y pantallas de PC y celular.

## Decisiones

Ver [ADR-0033](../../adr/0033-login-con-qr.md): `code` público + `secret` privado, canje atómico de un solo uso, CSRF solo en autorizar/rechazar, sesión con `OidcUser` y cambio de identificador de sesión.

## Archivos

- Contrato: `docs/api/openapi.yaml` (tag "Login con QR", 6 operaciones).
- Backend: migración `V4__create_qr_login.sql`; paquete `app.qrlogin` (`QrLogin`, `QrLoginRepository`, `QrLoginService`, `QrLoginController`, `IniciadorDeSesion`, DTOs, excepciones); `ApiExceptionHandler` (404 y 409); `SeguridadConfig` (CSRF); `UsuarioRepository.buscarPorId`; `app.qr-login.ttl`.
- Web: `frontend/qr-login.html`, `assets/qr-login.js` (celular), `assets/qr-login-pc.js` y tarjeta en `index.html` (computadora), `app.js` (vuelta desde Google), `styles.css`; `build.gradle` incluye `qr-login.html`.
- Tests: `QrLoginIT`, `IniciadorDeSesionTest`, `WebResourcesIT`.

## Resultado (2026-10-26)

- `.\tests.ps1`: BUILD SUCCESSFUL; JaCoCo 98 % de instrucciones y 94 % de ramas.
- Servidor levantado en el puerto 8081 con una base nueva (la anterior tenía una `V2` distinta de la de `main`; se renombró a `database-vieja`).
- Probado a mano: login con Google; el QR abre y autoriza entre dos navegadores de la misma computadora. Desde el celular falla con `ERR_CONNECTION_REFUSED` porque el QR apunta a `localhost`.
- Extensión: crea el enlace en el espacio del usuario si hay sesión y espacio (`extension/`, README, pruebas 13 a 16).

## Pendiente

- Probar la extensión a mano en Chrome y Firefox.
- Pregunta E9 al docente: el celular necesita una URL pública con HTTPS (túnel como ngrok o cloudflared, o un dominio) para escanear el QR e iniciar sesión con Google. Mientras tanto, la prueba se hace entre dos navegadores de la misma computadora.
- Aclarar con el equipo: al cambiar de espacio, el código de `main` vence los enlaces en vez de migrarlos.

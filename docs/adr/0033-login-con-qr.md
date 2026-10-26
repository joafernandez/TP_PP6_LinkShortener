# ADR-0033: Login con QR desde el celular

- **Estado:** Aceptada (aprobación del equipo, Etapa 2, parte C)
- **Origen:** requerimiento del Cliente: "login con Google o por QR"; el celular autoriza el ingreso de la computadora.

## Contexto

El Cliente pide iniciar sesión en la computadora escaneando un QR con el celular. El celular ya tiene (o inicia) sesión con Google y autoriza o rechaza. Los detalles no están en la consigna; el equipo decidió el diseño de abajo.

## Decisión

1. **Pedido de login** en la tabla `qr_login` (migración `V4`): `code` (público, va en el QR), `secret` (privado, solo lo tiene la computadora), estado `PENDING | AUTHORIZED | REJECTED | USED`, usuario que respondió y vencimiento. `EXPIRED` no se guarda: se calcula al consultar.
2. **Vigencia de 2 minutos** (`app.qr-login.ttl`, configurable). Pasado ese tiempo no se puede autorizar ni canjear.
3. **Un solo uso**: canjear es un `UPDATE ... WHERE estado = AUTHORIZED AND vence > ahora`. Si dos pedidos compiten, la base deja pasar solo a uno. Lo mismo vale para autorizar/rechazar desde `PENDING`.
4. **Quién puede qué:** `code` y `secret` son aleatorios (256 bits, `SecureRandom`). Ver el estado y canjear exigen el `secret` (encabezado `X-Qr-Secret`, comparado en tiempo constante); quien solo ve el QR (o lo fotografía) no puede iniciar sesión en la computadora. Autorizar y rechazar exigen la sesión de Google del celular.
5. **Sesión de la computadora:** al canjear se arma el mismo tipo de principal que el login con Google (`OidcUser`) y se guarda en la sesión HTTP, cambiando el identificador de sesión previo (contra fijación de sesión). El resto de la API no distingue cómo ingresó el usuario.
6. **CSRF:** crear, consultar y canjear los hace una computadora sin sesión y se protegen con el secreto, por eso se excluyen de CSRF. Autorizar y rechazar usan la cookie de sesión del celular y **sí exigen** `X-XSRF-TOKEN`.
7. **Pantallas:** la computadora muestra el QR y consulta cada 2 s (mensajes distintos para rechazado, vencido, usado o error). El QR codifica `{app.base-url}/qr-login.html?c={code}`; esa página pide Google si hace falta (guarda el código en `sessionStorage` para volver) y muestra "¿Autorizar el inicio de sesión en la computadora?" con Autorizar / Rechazar. La página quita el código de la barra de direcciones.
8. **Limpieza:** al crear un pedido se borran los vencidos hace más de una hora (se conservan un rato para informar "venció").

## Consecuencias

- Para que el celular llegue al servidor hace falta una dirección HTTPS pública (Google solo acepta HTTPS o localhost como redirect). Se resuelve con un túnel (por ejemplo ngrok o cloudflared) o un dominio: `APP_BASE_URL` y el URI de redirección de Google deben coincidir. Pendiente de confirmar con el docente (pregunta E9).
- Un atacante que vea el QR puede hacer que el *usuario* autorice sin querer (ingeniería social): por eso la pantalla del celular pide confirmar explícitamente y aclara que solo se autorice si el QR se escaneó desde la propia computadora.
- Sin sondeo largo ni websockets: consulta cada 2 s, simple y sin tecnologías nuevas.

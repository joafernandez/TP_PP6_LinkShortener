# ADR-0030 – Login con Google mediante Spring Security

- **Estado:** Aceptada
- **Fecha:** 2026-10-25
- **Etapa:** 2
- **Origen:** requerimiento del Cliente para la Etapa 2 (Camino 2) y aprobación del equipo para agregar Spring Security ([minuta 02](../etapa-2/minutas/minuta-02-elucidacion.md))

## Contexto
Los usuarios deben poder registrarse e iniciar sesión con Google. Si el login falla o se cancela, no se crea una sesión y se muestra un mensaje claro. El camino anónimo de la Etapa 1 tiene que seguir funcionando igual, también desde la extensión.

## Decisión
- **Spring Security con `oauth2Login`** (`spring-boot-starter-security-oauth2-client`; el nombre anterior `spring-boot-starter-oauth2-client` está marcado como obsoleto en Boot 4.1). Spring valida la respuesta de Google (`state`, `nonce` y firma del ID token).
- **Sesión del servidor por cookie** (`JSESSIONID`), sin tokens propios.
- **Todas las rutas siguen públicas.** Los endpoints que necesitan sesión la comprueban en su controller y responden **401 con ProblemDetail** (`SesionRequeridaException`), igual que el resto de los errores de la API, en lugar de redirigir a Google.
- **Registro del usuario:** `UsuarioOidcService` delega en el `OidcUserService` de Spring y luego llama a `UsuarioService.registrarSiEsNuevo`, que busca por el `sub` de Google y crea el usuario solo la primera vez ([ADR-0028](0028-modelo-de-usuarios-y-espacios.md)).
- **Login fallido o cancelado:** vuelve a `/?login=error` sin sesión; la web muestra el mensaje y permite seguir acortando como anónimo.
- **Cierre de sesión:** `POST /logout` responde 204.
- **CSRF activo**, en el modo para páginas con JavaScript (`csrf.spa()`): el servidor deja el token en la cookie `XSRF-TOKEN` y la web lo reenvía en el encabezado `X-XSRF-TOKEN`. Se excluye `/api/v1/links/**`: el alta anónima no usa la sesión, y la extensión no tiene esa cookie.
- **Credenciales** en las variables de entorno `GOOGLE_CLIENT_ID` y `GOOGLE_CLIENT_SECRET`, nunca en el repositorio. Sin ellas la aplicación arranca y solo el login falla.
- Se agregan `login` y `logout` a las palabras reservadas de los alias, porque Spring Security atiende esas rutas.

## Alternativas consideradas
- **Google Identity Services en el navegador**, enviando el ID token al backend: hay que validarlo a mano o con otra librería, carga un script de Google y no sirve para la extensión.
- **Proveedor externo** (Firebase, Auth0): agrega otro servicio y otra tecnología sin necesidad para esta escala.
- **Rutas protegidas por Spring Security con redirección a Google:** un cliente de la API recibiría una página HTML de Google en lugar de un 401 en JSON.
- **CSRF desactivado:** la sesión por cookie quedaría expuesta a pedidos falsificados desde otros sitios.

## Consecuencias
- Para probar el login real hay que crear un cliente OAuth en Google Cloud con la dirección de retorno `http://localhost:8081/login/oauth2/code/google`. Google exige HTTPS o `localhost` (minuta 02, E9).
- Los tests simulan el login con `spring-security-test` (`oidcLogin()`), sin llamar a Google.
- Las sesiones viven en memoria: al reiniciar el servidor hay que volver a iniciar sesión.
- `UsuarioService` consulta `EspacioRepository` y `Espacio` referencia a `Usuario`: los paquetes `usuario` y `espacio` dependen entre sí. Se acepta por ahora; si crece, el perfil puede moverse a un paquete propio.
- Mientras no exista el paso 2.4, un usuario con sesión sigue creando enlaces anónimos.

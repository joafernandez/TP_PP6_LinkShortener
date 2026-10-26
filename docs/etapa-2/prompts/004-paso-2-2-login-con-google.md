# 004 – Paso 2.2: login con Google

- **Fecha:** 2026-10-25
- **Herramienta:** Claude Code (VS Code)
- **Objetivo:** permitir iniciar y cerrar sesión con Google desde la web, registrar al usuario la primera vez y mostrar un mensaje claro si el login falla o se cancela, sin cambiar el camino anónimo.
- **Estado:** implementado y verificado, con Google simulado en los tests y con una cuenta real a mano.

## Prompt (resumido)

Tras guardar el paso 2.1 en git (PR #4), el equipo aprobó la lista de archivos del paso 2.2 con «listo, seguimos». Spring Security ya estaba aprobado para el login con Google.

## Diseño antes del código

- Contract-first: `openapi.yaml` agrega `GET /api/v1/me` (`UserResponse`: `name`, `email`, `namespace`), `POST /logout`, el esquema de seguridad `sesion` (cookie `JSESSIONID`) y la explicación del flujo y del token CSRF.
- Dependencias de Boot 4.1.1 verificadas en Maven Central: `spring-boot-starter-security-oauth2-client` y `spring-boot-starter-security-oauth2-client-test` (el nombre viejo `spring-boot-starter-oauth2-client` figura como obsoleto). Resuelven Spring Security 7.1.1.
- `SeguridadConfig`: todas las rutas públicas; `oauth2Login` con `failureUrl("/?login=error")`; logout con 204; CSRF en modo `spa()` excluyendo `/api/v1/links/**` (anónimo y usado por la extensión).
- `UsuarioOidcService` envuelve al `OidcUserService` de Spring (por composición, para poder testearlo) y registra al usuario con `UsuarioService.registrarSiEsNuevo`.
- `UsuarioController` responde 401 con ProblemDetail sin sesión (`SesionRequeridaException`, manejada en `ApiExceptionHandler`), en lugar de redirigir a Google.
- Web: botón «Iniciar sesión con Google», nombre del usuario, «Cerrar sesión» (reenvía la cookie `XSRF-TOKEN` en `X-XSRF-TOKEN`) y mensaje si vuelve con `?login=error`.
- `login` y `logout` se agregan a las palabras reservadas de los alias.

## Cambios

| Tipo | Archivos |
|---|---|
| Dependencias y configuración | `build.gradle`, `application.properties` de main (credenciales por variables de entorno, con un valor por defecto para arrancar sin ellas) y de test (valores ficticios) |
| Código | `common/config/SeguridadConfig`, `usuario/UsuarioService`, `UsuarioOidcService`, `UsuarioController`, `PerfilUsuario`, `SesionRequeridaException`, `dto/UsuarioResponse`; manejador 401 en `common/error/ApiExceptionHandler` |
| Web | `frontend/index.html`, `assets/app.js`, `assets/styles.css` |
| Tests | `UsuarioServiceTest` (5), `UsuarioOidcServiceTest` (2), `UsuarioControllerIT` (3), `SeguridadIT` (6) |
| Documentación | `openapi.yaml`, ADR-0030, `README.md` (cómo crear y cargar las credenciales), `CLAUDE.md` |

`ApiExceptionHandler` y las palabras reservadas no estaban en la lista aprobada: se tocaron porque el 401 con ProblemDetail y la ruta `/login` de Spring Security lo necesitaban.

## Verificación

- La herramienta `csrf()` de `spring-security-test` no reproduce el modo `spa()` (el test daba 403). Se reemplazó por el recorrido real: obtener la cookie `XSRF-TOKEN` con `GET /` y reenviarla en `X-XSRF-TOKEN`, que es lo que hace la web.
- `gradlew clean test`: **163 tests**, sin fallos. JaCoCo: **97,50 % de instrucciones**, **97,46 % de líneas**, **90,82 % de ramas**.
- `SeguridadIT` comprueba con la seguridad activa que siguen públicos el alta anónima (sin token CSRF), la redirección, el QR, la web, Swagger y `/v3/api-docs`; que el login redirige a Google; que un login cancelado vuelve a `/?login=error`; y que el logout exige el token CSRF.
- Servidor real, sin credenciales: web 200, `GET /api/v1/me` 401 con ProblemDetail, alta anónima 201 (`/2`), login 302 a `accounts.google.com`, login cancelado 302 a `/?login=error`.
- Al correr una sola clase de tests, el umbral de JaCoCo falla aunque los tests pasen: se documentó `-x jacocoTestCoverageVerification` en `CLAUDE.md`.

## Prueba con Google real

- El equipo creó el proyecto y el cliente OAuth en Google Cloud. Las credenciales se guardaron en un archivo fuera del repositorio, cargado como variables de entorno al levantar el backend.
- Al guardar ese archivo, el entorno de la IA mostró su contenido. Se avisó al equipo y se recomendó generar un secreto nuevo en Google Cloud después de las pruebas.
- Resultado: el login con una cuenta real funcionó; `GET /api/v1/me` devolvió nombre y email con `namespace: null`; con sesión iniciada, el acortado siguió funcionando como anónimo (`/3`, con QR), como se espera hasta el paso 2.4.

# 009 – Sección «Mis enlaces»

- **Fecha:** 2026-11-15
- **Herramienta:** Claude Code (VS Code)
- **Objetivo:** que el usuario con sesión vea y pruebe los enlaces de su espacio, sin cambiar ninguna funcionalidad existente.
- **Estado:** implementado y verificado. En la rama `feature/etapa2-transicion-espacio`, sin commit.

## Prompt (resumido)

Agregar «Mis enlaces» debajo del formulario, visible solo con sesión: URL original, URL corta, creación, vencimiento y estado (vigente o vencido); botones para copiar, abrir y ver el QR; del más reciente al más antiguo; mensaje si no hay enlaces; actualizar al crear un enlace o cambiar el espacio. Cada usuario ve solo los suyos. Solución lo más sencilla posible, sin tecnologías nuevas ni cambios de base de datos.

## Decisiones

- `GET /api/v1/me/links` (contrato primero en `openapi.yaml`) devuelve el `LinkResponse` existente. La URL corta usa el nombre principal del espacio.
- Devuelve los enlaces del espacio que siguen en la base, vigentes o vencidos; sin espacio, lista vacía; sin sesión, 401.
- El estado (vigente o vencido) lo calcula la web con `expiresAt`, la misma fecha que ya muestra el resultado del acortador.
- Sin cambios en la base, el acortador, el login, el QR, los vencimientos ni la transición del espacio.

## Cambios

- `EnlaceDeEspacioRepository.buscarPorEspacio`, `EnlaceDeEspacioService.listar` y `EnlaceDeEspacioController` (`GET`; el `POST` reutiliza el mismo armado de la respuesta).
- Web: sección «Mis enlaces» en `index.html`, `assets/app.js` y `assets/styles.css`.
- Test nuevo `MisEnlacesIT` (6): orden y datos, solo los propios (ni de otros usuarios ni anónimos), incluye vencidos, URL con el nombre nuevo tras cambiar el espacio, lista vacía y 401.

## Verificación

- `gradlew clean test`: **267 tests**, sin fallos. JaCoCo: 98,26 % de instrucciones, 97,91 % de líneas, 91,67 % de ramas.
- Sintaxis de `app.js` revisada con `node --check`. Servidor real: la página incluye la sección y `GET /api/v1/me/links` sin sesión responde 401.

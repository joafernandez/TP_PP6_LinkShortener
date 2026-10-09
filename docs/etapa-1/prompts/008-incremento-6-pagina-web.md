# 008 – Incremento 6: página web

- **Fecha de inicio:** 2026-10-08
- **Herramienta:** Codex
- **Objetivo:** implementar la pantalla web que acorta una URL y muestra la URL corta y su QR (HU-01 y HU-06), reutilizando el backend existente.
- **Estado:** implementado, verificado y guardado en Git a pedido del equipo, sin push.

## Pedido del equipo

El equipo tiene a cargo los incrementos 5 y 6 y pidió implementar y explicar en pasos pequeños, usando equivalencias con PHP. Tras cerrar el QR y dos correcciones del backend, pidió: «ok listo mejoras, sigamos».

Tras la implementación y las verificaciones, pidió «commitea», autorizando guardar juntos el código, las pruebas y la documentación. No pidió push.

## Diseño antes del código

- Aplicar ADR-0013: HTML, CSS y JavaScript sin framework, con fuentes en `frontend/`. Gradle incorpora esos archivos a `static/` dentro del JAR mediante `processResources`; Spring Boot sirve la página en `/`.
- Pantalla en español con etiqueta «Dirección a acortar», campo URL, botón ACORTAR, mensajes accesibles, enlace corto y QR.
- Crear enlaces con `POST /api/v1/links` y JSON `{url}`. Usar rutas relativas y el contrato existente, sin nuevos endpoints ni cambios de reglas.
- Mostrar la URL corta y la fecha `expiresAt` devueltas por el servidor. El cliente no fija el TTL ni genera alias.
- Cargar el QR mediante la imagen de `GET /api/v1/links/{alias}/qr`. Mostrar su estado de carga y conservar el enlace si la imagen falla.
- Mostrar envío pendiente, éxito, errores ProblemDetail y problemas de conexión. Evitar envíos repetidos mientras hay una petición pendiente y limpiar el resultado anterior al iniciar una nueva petición.
- Usar etiquetas, foco visible, regiones de estado y diseño adaptable. Insertar textos con `textContent`, sin interpretar contenido del servidor como HTML.
- Verificar el empaquetado y las rutas con integración; comprobar en navegador el formulario, errores y carga de imagen. Las pruebas manuales usan un proceso separado y HSQLDB en memoria.
- Durante la implementación, mantener el código, las pruebas y la documentación sin commit ni push hasta un pedido explícito del equipo. El pedido posterior de commit se registra arriba.

## Resultados

- Implementados `frontend/index.html`, `frontend/assets/styles.css` y `frontend/assets/app.js`, más la incorporación a `static/` mediante `processResources` en `backend/build.gradle`.
- Se agregó `WebResourcesIT` con dos pruebas: página de inicio y disponibilidad del HTML/CSS/JavaScript empaquetados, sin interferencia con la ruta del alias.
- Primera suite: 107 tests con un fallo en la lectura del HTML con tildes. MockMvc interpretó la respuesta con su codificación predeterminada, aunque el HTML declara UTF-8. Se corrigió el test para leer explícitamente UTF-8; se conservó la comprobación de «Dirección a acortar».
- Resultado final de `gradlew test bootJar --offline --no-daemon`, con el JDK 25 portable: **BUILD SUCCESSFUL**, **107 tests, 0 fallos, 0 errores y 0 omitidos**. El JAR incluye las tres fuentes del frontend como recursos estáticos.
- JaCoCo del backend: **96,72 % de instrucciones**, **96,17 % de líneas** y **93,42 % de ramas**. Esta cobertura no mide el JavaScript; sus recorridos se comprobaron en el navegador.
- Se levantó un JAR separado en `localhost:18086`, ligado a `127.0.0.1`, con HSQLDB en memoria. Por HTTP: `/`, CSS, JavaScript y `/v3/api-docs` respondieron 200; Swagger UI respondió su 302 habitual. La URL creada desde la pantalla respondió 302 con el destino esperado, sin seguir la redirección externa.
- En el navegador de Codex se verificaron: campo vacío con mensaje en español; creación y QR cargado (dimensiones naturales 300 × 300); repetición de la URL con alias nuevo; envío con Enter; FTP rechazado con el detalle del backend; rechazo de URL del propio servicio; limpieza del resultado anterior y recuperación del botón.
- Se inspeccionaron los anchos de escritorio y celular. En la comprobación móvil no hubo desbordamiento horizontal. Se guardaron capturas locales en `backend/build/web-desktop.jpg` y `web-mobile.jpg`, ignoradas por Git, y se restableció el tamaño del navegador.
- Para verificar fallas se usó una herramienta Java temporal en `backend/build/WebQrFailureProxy.java` (ignorada por Git), ligada a `127.0.0.1:18087`: sirvió la misma aplicación, demoró el POST y rechazó únicamente la imagen QR. Se observó el botón deshabilitado durante la petición, luego el enlace visible con aviso de QR fallido. Tras detener esa herramienta se comprobó el mensaje de pérdida de conexión y la recuperación del botón. Esta herramienta no integra el producto ni agrega dependencias.
- Las verificaciones usaron datos de ejemplo en memoria. No se modificó la base persistente ni el proceso que pudiera existir en 8081. No se probó con una cámara física de celular.
- Se actualizaron el README, la arquitectura, el índice de prompts y el contexto de continuación. Los procesos temporales de verificación se detuvieron. Código, pruebas y documentación se guardan juntos tras el pedido explícito de commit, sin push.

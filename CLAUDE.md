# CLAUDE.md – Link Shortener (TP Integrador PP6)

Guía para trabajar en este proyecto con asistencia de IA. Centraliza el contexto, las decisiones y las reglas de trabajo.

## 1. El proyecto

- **Qué es:** Servicio de Acortamiento y Gestión de Enlaces (Link Shortener). Es el TP integrador de Paradigmas de Programación VI (5.º año, Ingeniería en Informática).
- **Fuente oficial de requerimientos:** [docs/consigna.md](docs/consigna.md). Leerla antes de proponer cambios.
- **Dinámica:** el docente actúa como Cliente. Hay 3 etapas iterativas y los requerimientos de las Etapas 2 y 3 **no se conocen de antemano**. El diseño debe tolerar el cambio sin reescrituras masivas.
- **Etapa actual:** Etapa 1. No inventar ni anticipar requerimientos de etapas futuras.

### Requerimientos de la Etapa 1 (según la consigna)

- Recibir una URL válida y generar una URL corta única `http://{dominio_o_ip}/{alias}`. El alias debe ser lo más corto y fácil de recordar posible.
- Redirigir de forma transparente de la URL corta a la original.
- Vigencia de **60 minutos**: luego deja de redirigir y el alias puede reasignarse.
- Página web con el campo "dirección a acortar" y el botón **ACORTAR**.
- Extensión para **Chrome y Firefox** que acorte la página actual con un solo clic.
- La web y la extensión muestran la URL corta y un **código QR**.
- Persistencia con **JPA/Hibernate** sobre una base relacional.
- Backend **API REST** con Spring Boot. La web y la extensión son clientes del mismo backend.
- Por cada etapa: elucidación (minutas), diseño con **contrato OpenAPI antes del código**, implementación asistida por IA y QA (tests unitarios y de integración, cobertura, buenas prácticas REST y JPA).

## 2. Forma de trabajo (obligatoria)

1. **Ciclo por cambio:** analizar → proponer → revisar → implementar → probar.
2. **Antes de crear o modificar archivos**, mostrar la lista de archivos afectados y esperar aprobación.
3. **Desarrollo incremental:** un incremento pequeño y verificable por vez. Nada de cambios masivos.
4. **No borrar ni reemplazar archivos** sin justificarlo.
5. **No inventar** endpoints, entidades, campos ni reglas que no surjan de la consigna o de una decisión registrada.
6. **Ambigüedades funcionales:** no resolverlas por cuenta propia. Se registran como pendientes y se llevan al docente en una minuta.
7. **Tecnologías nuevas:** no agregarlas sin justificar la necesidad y obtener aprobación.
8. **Dependencias:** cada una se agrega recién en el incremento que la usa.
9. Separar siempre: lo que **exige la consigna**, lo que **decide el equipo** (con recomendación) y lo que **se propone**.
10. **Prompts:** guardar los prompts relevantes usados con la IA en `docs/etapa-N/prompts/`, porque son un entregable.

## 3. Stack aprobado

| Elemento | Decisión |
|---|---|
| Lenguaje | Java, JDK **25** (LTS) |
| Framework | Spring Boot **4.1.1** (Spring Web MVC, Spring Data JPA starter) |
| Build | Gradle mediante el wrapper (`gradlew`, Gradle 9.7.1). No hace falta instalar Gradle |
| Persistencia | JPA/Hibernate con `EntityManager` y JPQL/HQL. **Sin capa Repository** |
| Base de datos | HSQLDB en modo archivo (`database/`). En los tests, HSQLDB en memoria |
| Esquema | Flyway con migraciones versionadas (se incorpora en el incremento 3) |
| Validación | Bean Validation, `spring-boot-starter-validation` (incremento 3) |
| API | REST con `@RestController`, bajo `/api/v1` |
| Contrato | OpenAPI contract-first en `docs/api/openapi.yaml`, más springdoc para Swagger UI (incremento 3) |
| Tests | JUnit 5, Spring Boot Test, MockMvc. Cobertura con JaCoCo |
| Web | HTML, CSS y JS sin framework, servida por Spring Boot |
| Extensión | Manifest V3, un solo código para Chrome y Firefox |
| Versionado | git, con un tag por etapa (`etapa-1`, `etapa-2`, ...) |

**No agregar sin aprobación:** Docker, Redis, PostgreSQL, React, Angular, microservicios, autenticación, capa Repository ni ninguna otra tecnología no listada.

## 4. Arquitectura

```
[Web] ─┐
       ├── REST /api/v1 ──► Controller → Service → EntityManager ──► HSQLDB
[Ext] ─┘                      (DTOs)     (reglas)     (JPQL)
[Navegador] ── GET /{alias} ──► RedirectController → Service
```

**Reglas de dependencia:**
- Los controllers solo conocen services y DTOs. No contienen lógica de negocio.
- La lógica de negocio vive en los services (`@Transactional`).
- Las entidades nunca se exponen en la API: siempre se usan DTOs.
- Los clientes (web y extensión) solo conocen el contrato REST.

### Paquetes: organizados por funcionalidad, raíz `app`

```
app/
├── LinkShortenerApplication.java
├── link/       entidad Link, LinkService, LinkController, RedirectController, dto/, alias/
├── qr/         generación del QR (si se confirma D8 en el servidor)
└── common/     config/ (propiedades, Clock, etc.) y error/ (@RestControllerAdvice)
```

Una funcionalidad nueva va en un **paquete hermano** (por ejemplo `app.stats`), sin modificar los existentes salvo que sea necesario.

### Puntos de extensión previstos

- `AliasGenerator` como interfaz, para cambiar la estrategia de alias sin tocar el service.
- `@ConfigurationProperties` para base-url, duración (60 min), largo del alias y palabras reservadas.
- `java.time.Clock` inyectado, para testear el vencimiento sin esperar.
- DTOs separados de las entidades.
- Versión de API `/api/v1`.

## 5. Convenciones técnicas

- **REST:**
  - Recursos en plural (`/api/v1/links`).
  - `201 Created` con encabezado `Location` al crear.
  - Errores con `ProblemDetail` (RFC 9457).
  - **Redirección con `302`, nunca `301`**, porque el navegador cachea el 301 y los alias se reasignan.
- **Ruta del alias:**
  - `/{alias}` en la raíz, restringida a **un segmento alfanumérico** (`/{alias:[A-Za-z0-9]+}`) para no chocar con los archivos estáticos ni con Swagger.
  - Una **lista configurable de palabras reservadas** (`api`, `error`, `v3`, `swagger`, ...) que el generador nunca usa.
- **JPA:**
  - Fechas con `Instant`.
  - `spring.jpa.open-in-view=false`.
  - JPQL con parámetros nombrados, nunca concatenando strings.
  - El esquema lo manejan las migraciones, no `ddl-auto`.
- **Spring:**
  - Inyección por constructor.
  - `@Transactional` en el service, no en el controller.
- **Extensión:**
  - **No se usa CORS.** En Firefox el origen de la extensión es un UUID distinto en cada instalación.
  - Se usa `optional_host_permissions` y, en la pantalla de opciones, `permissions.request()` con el host configurado.
  - `permissions.request()` debe llamarse **directamente en el handler del clic de Guardar, antes de cualquier `await`**. Si no, el navegador (sobre todo Firefox) lo rechaza porque deja de considerarlo una acción del usuario.
- **Tests:**
  - Unitarios para la lógica (con un `Clock` fijo).
  - De integración con `@SpringBootTest` y MockMvc, idealmente uno por criterio de aceptación.

## 6. Decisiones

### Supuestos del equipo, pendientes de confirmación del docente

Las ambigüedades D1 a D8 están en [docs/etapa-1/minutas/minuta-01-elucidacion.md](docs/etapa-1/minutas/minuta-01-elucidacion.md). El equipo adoptó provisoriamente sus propuestas, y **se implementan**. Si el docente responde distinto, se crea un ADR nuevo y se ajusta lo afectado.

| # | Supuesto adoptado | ADR |
|---|---|---|
| D1 | Alias aleatorio de 5 caracteres, alfabeto de 56 sin ambiguos (`23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz`), largo configurable | 0016 |
| D2 | Cada pedido genera un alias nuevo, sin reutilizar ni renovar | 0017 |
| D3 | Vencido o inexistente: HTTP 404 con página HTML simple, sin distinguir entre ambos | 0018 |
| D4 | Se rechaza (400) una URL del propio servicio (mismo esquema, host y puerto que `app.base-url`) | 0019 |
| D5 | URL válida: absoluta, `http`/`https`, con host, hasta 2048 caracteres, sin verificar que exista | 0020 |
| D6 | Sin historial: alias `UNIQUE` y reasignación con una actualización atómica del registro vencido | 0021 |
| D7 | `app.base-url` configurable, y pantalla de opciones en la extensión. HTTP por defecto | 0022 |
| D8 | QR generado en el backend con ZXing (`GET /api/v1/links/{alias}/qr`, PNG) | 0023 |

**D8 es el supuesto con más riesgo**: la consigna dice que los clientes "generan" el QR.

### Tomadas

Las decisiones técnicas aprobadas están registradas como ADR (ADR-0001 a ADR-0015), y los supuestos funcionales como ADR-0016 a ADR-0023. El índice está en [docs/adr/README.md](docs/adr/README.md).

## 7. Estructura del repositorio

```
TP_PP6_LinkShortener/
├── CLAUDE.md           esta guía
├── README.md           cómo compilar y correr
├── backend/            Spring Boot (Gradle)
├── frontend/           página web (se copia a los recursos estáticos al compilar)
├── extension/          extensión MV3 Chrome/Firefox
├── database/           archivos de HSQLDB (ignorados por git)
└── docs/
    ├── consigna.md
    ├── etapa-1/minutas/    reuniones con el Cliente
    ├── etapa-1/prompts/    prompts usados con la IA
    ├── api/openapi.yaml    contrato de la API
    └── adr/                decisiones de diseño
```

## 8. Comandos

Desde `backend/` (en Windows, `gradlew.bat`):

```bash
./gradlew test                                  # tests y reporte de cobertura
./gradlew bootRun                               # levanta en http://localhost:8080
./gradlew bootRun --args='--server.port=8081'   # en otro puerto
```

- Reporte de cobertura: `backend/build/reports/jacoco/test/html/index.html`
- Swagger UI: `http://localhost:8080/swagger-ui.html` (con la aplicación levantada)
- La URL base de los enlaces se configura con la variable `APP_BASE_URL` (por ejemplo `http://192.168.1.50`). Si se cambia el puerto, hay que ajustarla también.
- Los tests de integración terminan en `IT` y los unitarios en `Test`. Gradle ejecuta todos con `gradlew test`.
- Ubicación de la base: `database/`. Se puede cambiar con la variable de entorno `APP_DB_PATH`.

## 9. Plan de la Etapa 1

| # | Incremento | Estado |
|---|---|---|
| 0 | Minuta de elucidación (D1 a D8) | ✅ Hecho. Supuestos adoptados, falta confirmación del docente |
| 1 | Diseño: requerimientos e historias, `arquitectura.md`, ADRs, `openapi.yaml` | ✅ Hecho ([requerimientos](docs/etapa-1/requerimientos.md), [arquitectura](docs/arquitectura.md), [OpenAPI](docs/api/openapi.yaml), ADRs 0001 a 0023, [prompts](docs/etapa-1/prompts/README.md)) |
| 2 | Base técnica: proyecto Spring Boot, HSQLDB, test de arranque, git | ✅ Hecho |
| 3 | Entidad `Link`, migración, `AliasGenerator`, `POST /api/v1/links` | ✅ Hecho (54 tests, 95% de cobertura; Swagger UI en `/swagger-ui.html`) |
| 4 | Redirección `GET /{alias}`, página 404, `LinkService.resolve` | ✅ Hecho (66 tests, 95% de cobertura) |
| 5 | Código QR | ✅ Hecho: servicio y endpoint PNG, 404 JSON, 76 tests pasan y verificación HTTP |
| 6 | Página web | ⏭️ Siguiente |
| 7 | Extensión Chrome/Firefox con pantalla de opciones | Pendiente |
| 8 | QA: cobertura, revisión REST y JPA, contraste con OpenAPI, tag `etapa-1` | Pendiente |

**Mantener esta tabla actualizada al cerrar cada incremento.**

## 10. Estado actual y cómo retomar

**Último incremento cerrado:** el 5, código QR (al 2026-10-08). Ver `git log` para los commits.

**Forma de trabajo:** implementar y explicar en pasos pequeños usando equivalencias con PHP. El servicio del QR se guardó primero en el commit `3afbc89`, a pedido del usuario, antes de conectar el endpoint. El incremento 5 se cerró con 76 tests y una comprobación HTTP del JAR. Registro: [prompt 005](docs/etapa-1/prompts/005-incremento-5-codigo-qr.md).

**Corrección de reintentos (2026-10-08):** se reintentan errores de persistencia solo si se confirma la restricción UNIQUE del alias; los demás se propagan conservando la excepción. Se verificó con 89 tests y se guardó en el commit `c02e915` a pedido del usuario. Registro: [prompt 006](docs/etapa-1/prompts/006-reintentos-por-colision-de-alias.md).

**Última corrección (2026-10-08):** se implementó la versión simplificada del punto 2, aprobada por el usuario: largo del alias entre 1 y 16, duración positiva y URL base HTTP/HTTPS con host válido, comprobados al arrancar mediante `AppProperties`. Suite actual: **105 tests pasan**; JaCoCo **96,72 % de instrucciones**, **96,17 % de líneas** y **93,42 % de ramas**. Registro: [prompt 007](docs/etapa-1/prompts/007-validacion-basica-de-configuracion.md). Tras verificar la corrección y explicar la validación del TTL, el usuario pidió «commitea». El cambio se guarda en Git con sus pruebas y documentación; el push sigue sin autorización.

### Qué funciona hoy
- `POST /api/v1/links` crea enlaces: 201 con `Location` y `LinkResponse`, y 400 o 503 con ProblemDetail.
- `GET /{alias}` redirige con 302 a la URL original. Si el enlace está vencido o no existe, responde 404 con una página HTML (`resources/pages/link-not-found.html`).
- La generación de alias, la validación de URL, la reasignación atómica de alias vencidos y el vencimiento a los 60 minutos están implementados y testeados.
- Flyway crea la tabla `link` (migración `V1__create_link.sql`).
- Swagger UI en `/swagger-ui.html`.
- `GET /api/v1/links/{alias}/qr` entrega un PNG de 300 × 300 píxeles con la URL corta. Un alias vencido o inexistente devuelve 404 ProblemDetail en JSON. Ambas respuestas incluyen `Cache-Control: no-store`.
- Pedir el QR no crea registros ni renueva el vencimiento. Swagger documenta PNG, ProblemDetail y encabezados; un test comprueba esta documentación.
- La configuración básica se valida antes de aceptar pedidos: largo del alias, duración positiva y URL base HTTP/HTTPS con host. Si falla, Spring detiene el arranque con un mensaje en español.
- **Pendiente:** página web y extensión.

### Próximo paso: incremento 6 (página web, ADR-0013)
El usuario tiene a cargo los incrementos 5 y 6. El plan del 6 es HTML, CSS y JavaScript sin framework, servido por el mismo Spring Boot:
- Crear el frontend con un campo «dirección a acortar», botón ACORTAR, zona de mensajes, URL corta y QR.
- El formulario llama a `POST /api/v1/links`; con el alias devuelto carga `GET /api/v1/links/{alias}/qr`. Usar rutas relativas, sin fijar host ni puerto en JavaScript.
- Gradle copia los archivos de `frontend/` a los recursos estáticos al construir, según ADR-0013. No duplicar los archivos fuente dentro del backend.
- Mostrar estados de envío, éxito, errores del backend, conexión y carga del QR; conservar el enlace creado si falla la imagen. Cuidar etiquetas, teclado y presentación en celular.
- Verificar recursos con integración y el recorrido completo en un navegador. Mantener disponibles Swagger, la API y la redirección.
- Presentar los archivos concretos antes del siguiente paso de implementación, mantener las explicaciones con equivalencias PHP y registrar la sesión en el siguiente prompt disponible (008). Los prompts 006 y 007 corresponden a correcciones del backend. No se inició todavía este incremento.

### Pendientes abiertos
- **Confirmar con el docente** los supuestos D1 a D8 (ADR 0016 a 0023). El más riesgoso es D8 (QR en el servidor): conviene confirmarlo **antes** del incremento 5.
- Registrar cada sesión de trabajo en `docs/etapa-1/prompts/`. La última registrada es la 007 (validación básica de configuración).

### Lecciones técnicas (para no repetir errores)
- **Spring Boot 4.1.1** usa starters modulares: `spring-boot-starter-webmvc`, `-flyway`, `-validation`, y sus variantes `-test`. Flyway necesita además `org.flywaydb:flyway-database-hsqldb`.
- **springdoc 3.1.1** es la versión compatible con Boot 4.x. La rama 2.x es para Boot 3.
- **`Instant` en Hibernate y HSQLDB:** la columna tiene que ser `TIMESTAMP(6)` sin zona horaria (Hibernate normaliza a UTC). Con `WITH TIME ZONE`, la validación del esquema falla.
- **Precisión:** el reloj de Windows tiene 7 decimales y la base guarda 6. `LinkService` trunca a `ChronoUnit.MICROS`.
- **Reintentos de alias:** cada intento corre en su propia transacción (`TransactionTemplate`). Si no, un choque de `UNIQUE` dejaría la transacción marcada para rollback.
- **Clasificación de colisiones:** solo reintentar una `ConstraintViolationException` en la cadena de causas con SQLState `23505` y restricción `uk_link_alias` (o `PUBLIC.uk_link_alias`), sin distinguir mayúsculas. Los demás errores se propagan. La integración comprobó que HSQLDB informa `UK_LINK_ALIAS` y que, al usar IDENTITY, la inserción duplicada puede fallar ya en `persist`, antes del `flush` explícito.
- **Tests:**
  - MockMvc se arma con `MockMvcBuilders.webAppContextSetup(context)`.
  - Los beans se reemplazan con `@MockitoBean`.
  - El reloj se controla con `app.support.MutableClock`. Para usarlo, se importa `@Import(app.support.TestClockConfig.class)`, que lo registra como `@Primary` con la hora `TestClockConfig.T0`.
  - La base se limpia con `JdbcTemplate` en `@BeforeEach`.
- **Errores HTML vs. JSON:** un `@ExceptionHandler` local de un controller tiene prioridad sobre el `@RestControllerAdvice` global. Así se resuelve que la redirección responda HTML y la API ProblemDetail.
- **Una migración Flyway aplicada no se edita.** `V1` se corrigió solo porque todavía no se había aplicado en ninguna base persistente. A partir de ahora, cualquier cambio de esquema va en `V2`, `V3`, etc.

### Entorno local
- En esta copia, la consola usa **JDK 21**. Gradle requiere **JDK 25**, que se preparó como copia portable de Adoptium en `backend/.gradle/toolchains/jdk-25.0.4.1+1/` (ignorada por git), con su SHA256 verificado. No se cambió el Java del sistema. Para los tests, desde `backend/`, establecer `$env:JAVA_HOME = (Resolve-Path '.\.gradle\toolchains\jdk-25.0.4.1+1').Path` solo en la sesión de PowerShell y ejecutar `.\gradlew.bat test --no-daemon`.
- El **puerto 8080** puede estar ocupado por otro proceso Java ajeno al proyecto. En ese caso:
  ```bash
  APP_BASE_URL=http://localhost:8081 ./gradlew bootRun --args='--server.port=8081'
  ```
- Después de probar a mano, detener el proceso que quedó escuchando en el puerto usado.

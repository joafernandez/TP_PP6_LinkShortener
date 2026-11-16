# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# Link Shortener (TP Integrador PP6)

Guía para trabajar en este proyecto con asistencia de IA. Centraliza el contexto, las decisiones y las reglas de trabajo.

## 1. El proyecto

- **Qué es:** Servicio de Acortamiento y Gestión de Enlaces (Link Shortener). Es el TP integrador de Paradigmas de Programación VI (5.º año, Ingeniería en Informática).
- **Fuente oficial de requerimientos:** [docs/consigna.md](docs/consigna.md) para la Etapa 1. Los requerimientos de la Etapa 2 están en el [prompt 001 de la Etapa 2](docs/etapa-2/prompts/001-analisis-entrega-2.md). Leerlos antes de proponer cambios.
- **Dinámica:** el docente actúa como Cliente. Hay 3 etapas iterativas y los requerimientos de cada etapa **no se conocen de antemano**. El diseño debe tolerar el cambio sin reescrituras masivas.
- **Etapa actual:** Etapa 2 (ver sección 9.1). No inventar ni anticipar requerimientos de la Etapa 3.

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

### Requerimientos de la Etapa 2 (resumen)

- **Camino 1, sin cuenta:** código de 1 carácter siempre que sea posible, asignado de forma incremental (`/1`, `/2`…), reutilizando los vencidos y pasando a 2 caracteres al agotarse.
- **Camino 2, con cuenta:** login con Google y login con QR escaneado desde el celular (con autorización explícita). Cada usuario tiene un único espacio de nombres (`/melina/1`), único, en minúsculas y cambiable; al cambiarlo, los enlaces funcionan con el nombre nuevo y el anterior sigue funcionando y reservado 60 minutos (requerimiento posterior, ADR-0034). Código automático o personalizado (`/melina/final`), único solo dentro del espacio.
- **Extensión:** con sesión, acorta en el espacio del usuario; sin sesión, sigue en modo anónimo.

## 2. Forma de trabajo (obligatoria)

1. **Ciclo por cambio:** analizar → proponer → revisar → implementar → probar.
2. **Antes de crear o modificar archivos**, mostrar la lista de archivos afectados y esperar aprobación.
3. **Desarrollo incremental:** un incremento pequeño y verificable por vez. Nada de cambios masivos.
4. **No borrar ni reemplazar archivos** sin justificarlo.
5. **No inventar** endpoints, entidades, campos ni reglas que no surjan de la consigna o de una decisión registrada.
6. **Ambigüedades funcionales:** no resolverlas por cuenta propia. Se registran como pendientes y se llevan al docente en una minuta.
7. **Tecnologías nuevas:** no agregarlas sin justificar la necesidad y obtener aprobación.
8. **Dependencias:** cada una se agrega recién en el incremento que la usa.
9. Separar siempre: lo que **exige la consigna**, lo que **decide el equipo** (con recomendación) y lo que **se propone**. Los límites técnicos (por ejemplo, 15 caracteres) se documentan como decisión del equipo, no del docente.
10. **Prompts:** guardar los prompts relevantes usados con la IA en `docs/etapa-N/prompts/`, porque son un entregable.
11. **Git:** trabajar en una rama por paso (`feature/...`) y llevarla a `main` con un PR. Hacer commit o push solo cuando se pida. Nunca subir credenciales ni archivos de `database/`.
12. **Explicaciones:** en pasos chicos, con equivalencias con PHP.

## 3. Stack aprobado

| Elemento | Decisión |
|---|---|
| Lenguaje | Java, JDK **25** (LTS) |
| Framework | Spring Boot **4.1.1** (Spring Web MVC, Spring Data JPA starter) |
| Build | Gradle mediante el wrapper (`gradlew`, Gradle 9.7.1). No hace falta instalar Gradle |
| Persistencia | JPA/Hibernate con `EntityManager` y JPQL en Repository concretos (ADR-0024) |
| Base de datos | HSQLDB en modo archivo (`database/`). En los tests, HSQLDB en memoria |
| Esquema | Flyway con migraciones versionadas (`V1` a `V4`) |
| Validación | Bean Validation, `spring-boot-starter-validation` |
| API | REST con `@RestController`, bajo `/api/v1` |
| Contrato | OpenAPI contract-first en `docs/api/openapi.yaml`, más springdoc para Swagger UI |
| Tests | JUnit 5, Spring Boot Test, MockMvc, `spring-security-test`. Cobertura con JaCoCo y umbral obligatorio (ADR-0025) |
| Web | HTML, CSS y JS sin framework, servida por Spring Boot |
| Extensión | Manifest V3, un solo código para Chrome y Firefox |
| Autenticación | Spring Security 7 con `oauth2Login` para Google, sesión por cookie y CSRF en modo `spa()` (ADR-0030) |
| QR | ZXing, generado en el servidor (ADR-0023) |
| Versionado | git, con un tag por etapa (`etapa-1`, `etapa-2`, ...) |

**No agregar sin aprobación:** Docker, Redis, PostgreSQL, React, Angular, microservicios, túneles o hosting, ni ninguna otra tecnología no listada. Spring Security quedó aprobado en la Etapa 2 solo para el login con Google.

## 4. Arquitectura

```
[Web] ─┐                                        ┌─ Spring Security (oauth2Login con Google, sesión, CSRF)
       ├── REST /api/v1 ──► Controller → Service → Repository → EntityManager ──► HSQLDB
[Ext] ─┘                      (DTOs)     (reglas)    (persistencia y JPQL)
[Navegador] ── GET /{alias}             ──► RedireccionController → EnlaceService
[Navegador] ── GET /{espacio}/{código}  ──► RedireccionController → EnlaceDeEspacioService
[Celular]   ── qr-login.html ─► /api/v1/auth/qr-logins/{código}/authorize ─► la PC canjea su sesión
```

**Reglas de dependencia:**
- Los controllers solo conocen services y DTOs. No contienen lógica de negocio. Si un endpoint necesita sesión, la comprueba con `@AuthenticationPrincipal OidcUser` y lanza `SesionRequeridaException` (401 con ProblemDetail): Spring Security deja todas las rutas públicas y no redirige a Google.
- La lógica de negocio y la coordinación de transacciones viven en los services (`TransactionTemplate` para cada intento de creación y `@Transactional` para resolver).
- El Repository concentra `EntityManager`, JPQL y la clasificación de errores de persistencia (`common/persistencia/RestriccionUnica` reconoce la violación de una restricción UNIQUE concreta). Participa en las transacciones del service, sin generar alias ni calcular fechas.
- Las entidades nunca se exponen en la API: siempre se usan DTOs. Los DTOs mantienen los nombres JSON en inglés con `@JsonProperty` y `@Schema`.
- Los clientes (web y extensión) solo conocen el contrato REST.

### Paquetes: organizados por funcionalidad, raíz `app`

```
app/
├── enlace/      enlaces anónimos (Enlace) y de espacio (EnlaceDeEspacio, tabla propia namespace_link),
│                services, Repository, RedireccionController, validadores, alias/ (GeneradorCodigoSecuencial)
├── espacio/     espacio de nombres del usuario: elegir, cambiar y validar; NombreDeEspacio guarda el nombre principal
│                y los anteriores en transición de 60 minutos (tabla namespace_name, ADR-0034)
├── usuario/     usuario de Google, GET /api/v1/me, puente entre el login de Google y la base
├── qrlogin/     login con QR: pedido de un solo uso, autorización desde el celular, canje de sesión en la PC
├── qr/          PNG del QR de un enlace (anónimo o de espacio)
└── common/      config/ (AppProperties, Clock, SeguridadConfig), error/ (ApiExceptionHandler), persistencia/
```

- `enlace` depende de `espacio` (`EnlaceDeEspacio` referencia a `Espacio`, y la redirección busca el espacio por cualquier nombre vigente). Desde el ADR-0034, `espacio` ya no depende de `enlace`. Se acepta por ahora (ADR-0032).
- Una funcionalidad nueva va en un **paquete hermano**, sin modificar los existentes salvo que sea necesario.

### Puntos de extensión

- `GeneradorAlias.generar(Set<String> ocupados)`: recibe los códigos vigentes y devuelve el menor libre (ADR-0026 y 0027). Lo usan el camino anónimo y los espacios.
- `@ConfigurationProperties` (`AppProperties`): `app.base-url`, `app.link.ttl`, `app.alias.*`, `app.namespace.reserved`, `app.qr-login.ttl`.
- `java.time.Clock` inyectado, para testear vencimientos sin esperar.

## 5. Convenciones técnicas

- **REST:**
  - Recursos en plural (`/api/v1/links`). Lo del usuario con sesión va bajo `/api/v1/me`.
  - `201 Created` con encabezado `Location` al crear.
  - Errores con `ProblemDetail` (RFC 9457), con mensajes claros en español.
  - **Redirección con `302`, nunca `301`**, porque el navegador cachea el 301 y los alias se reasignan.
- **Rutas públicas:**
  - `/{alias:[A-Za-z0-9]+}` (anónima) y `/{espacio}/{código}` (de espacio). Los segmentos no admiten puntos, para no chocar con los archivos estáticos ni con Swagger, y las rutas literales (`/v3/api-docs`) tienen prioridad.
  - Palabras reservadas configurables: `app.alias.reserved` (el generador nunca las usa) y `app.namespace.reserved` (no se pueden elegir como espacio).
  - Espacio y código se tratan en minúsculas.
- **Seguridad:**
  - CSRF en modo `spa()`: la web lee la cookie `XSRF-TOKEN` y la reenvía en `X-XSRF-TOKEN`.
  - Excluidos de CSRF: `/api/v1/links/**` (alta anónima, usada por la extensión) y crear, consultar y canjear un login con QR (se protegen con el secreto del encabezado `X-Qr-Secret`). Todo lo demás que modifica datos exige el token.
  - Credenciales de Google solo por las variables `GOOGLE_CLIENT_ID` y `GOOGLE_CLIENT_SECRET`. Sin ellas la aplicación arranca y solo falla el login.
- **JPA:**
  - Fechas con `Instant`, truncadas a microsegundos.
  - `spring.jpa.open-in-view=false`.
  - JPQL con parámetros nombrados, nunca concatenando strings.
  - El esquema lo manejan las migraciones, no `ddl-auto`. La unicidad que pide el Cliente se garantiza en la base.
- **Spring:**
  - Inyección por constructor. Si una clase tiene un segundo constructor para tests, el público lleva `@Autowired`.
  - Transacciones coordinadas en el service, no en el controller; el Repository participa en ellas.
- **Extensión:**
  - **No se usa CORS.** En Firefox el origen de la extensión es un UUID distinto en cada instalación.
  - Se usa `optional_host_permissions` y, en la pantalla de opciones, `permissions.request()` con el host configurado.
  - `permissions.request()` debe llamarse **directamente en el handler del clic de Guardar, antes de cualquier `await`**. Si no, el navegador (sobre todo Firefox) lo rechaza porque deja de considerarlo una acción del usuario.
- **Tests:**
  - Unitarios para la lógica (con un `Clock` fijo y mock del Repository), sin simular JPA en el service.
  - De integración con `@SpringBootTest` y MockMvc, idealmente uno por criterio de aceptación.

## 6. Decisiones

Todas las decisiones están registradas como ADR en [docs/adr/](docs/adr/README.md): técnicas (0001 a 0015, 0024, 0025, 0027 a 0035) y supuestos funcionales (0016 a 0023 de la Etapa 1, y 0026 de la Etapa 2). Una decisión no se edita: si cambia, se crea un ADR nuevo que la reemplaza.

### Supuestos pendientes de confirmación del docente

- **Etapa 1:** D1 a D8 en la [minuta 01](docs/etapa-1/minutas/minuta-01-elucidacion.md). D1 fue reemplazado por el ADR-0026. **D8 es el más riesgoso:** la consigna dice que los clientes "generan" el QR, y se genera en el servidor.
- **Etapa 2:** E1 a E10 en la [minuta 02](docs/etapa-2/minutas/minuta-02-elucidacion.md), con los criterios que aprobó el equipo. La más importante es **E9**: Google solo acepta HTTPS o `localhost`, así que el login con QR desde el celular necesita una URL pública con HTTPS.

## 7. Estructura del repositorio

```
TP_PP6_LinkShortener/
├── backend/            Spring Boot (Gradle)
├── frontend/           página web: index.html, qr-login.html y assets/ (Gradle las copia al JAR)
├── extension/          extensión MV3 Chrome/Firefox (README con las pruebas manuales)
├── database/           archivos de HSQLDB (ignorados por git, salvo .gitkeep)
└── docs/
    ├── consigna.md
    ├── etapa-1/ y etapa-2/   minutas y prompts usados con la IA
    ├── api/openapi.yaml      contrato de la API
    └── adr/                  decisiones de diseño
```

## 8. Comandos

Desde `backend/` (en Windows, `gradlew.bat`):

```powershell
.\gradlew.bat test                                    # todos los tests, reporte y umbral de cobertura
.\gradlew.bat test --tests "app.enlace.EnlacesDeEspacioIT" -x jacocoTestCoverageVerification   # un solo test
.\gradlew.bat bootRun --args="--server.port=8081"     # levantar en el 8081
```

- Con un solo test hay que agregar `-x jacocoTestCoverageVerification`: si no, el umbral de cobertura (ADR-0025) falla aunque el test pase, porque mide solo esa clase.
- **Atajo en Windows:** `.\tests.ps1` corre todos los tests desde cero y abre los reportes de tests y de cobertura.
- Reportes: `backend/build/reports/tests/test/index.html` y `backend/build/reports/jacoco/test/html/index.html`.
- Swagger UI: `http://localhost:8081/swagger-ui.html` (con la aplicación levantada).
- Los tests de integración terminan en `IT` y los unitarios en `Test`.
- Los tests que necesitan la seguridad real arman MockMvc con `.apply(springSecurity())`, simulan Google con `oidcLogin()` y, para CSRF, obtienen la cookie `XSRF-TOKEN` con `GET /` y la reenvían en `X-XSRF-TOKEN` (la herramienta `csrf()` no reproduce el modo `spa()`).

### Levantar con login de Google (la forma que usa el equipo)

```powershell
cd C:\TP_PP6_LinkShortener\backend
. C:\Users\joana\linkshortener-google.ps1      # define GOOGLE_CLIENT_ID y GOOGLE_CLIENT_SECRET; está fuera del repositorio
$env:APP_BASE_URL = "http://localhost:8081"
.\gradlew.bat bootRun --args="--server.port=8081"
```

- Sin el archivo de credenciales, todo funciona salvo el login. Cada integrante tiene su propia copia de ese archivo.
- La app de Google está en modo **Prueba**: solo entran los Gmail cargados como usuarios de prueba en Google Cloud (Google Auth Platform → Público).
- `APP_BASE_URL` arma las URL cortas y debe coincidir con la dirección usada. Si se cambia el puerto, ajustarla también. Con la IP de la PC, otros dispositivos de la misma red pueden usar el camino anónimo, pero no el login con Google.
- Para incorporar cambios de la web o del código, detener el servidor y volver a ejecutar `bootRun`. Si lo levantó Claude de fondo, detener el proceso que escucha en el puerto.

### Ver la base de datos
- Ubicación: `database/` (se cambia con `APP_DB_PATH`). Las fechas están en **UTC** (hora argentina + 3).
- **Administrador visual de HSQLDB:** desde `backend/`, `.\bd.ps1`. Requiere el **servidor apagado**, porque HSQLDB en modo archivo bloquea la base para un solo proceso. Usarlo solo para consultar y cerrarlo antes de volver a levantar el servidor.
- No editar a mano `linkshortener.script` ni `linkshortener.log`.

## 9. Plan de la Etapa 1

✅ Completa (incrementos 0 a 8) y en `main`. Detalle en [docs/etapa-1/prompts/](docs/etapa-1/prompts/README.md) y en el [informe de QA](docs/etapa-1/qa.md).

## 9.1 Plan de la Etapa 2

| Parte | Contenido | Estado |
|---|---|---|
| A | Códigos secuenciales para anónimos (ADR-0026 y 0027). [Prompt 002](docs/etapa-2/prompts/002-parte-a-codigos-secuenciales.md) | ✅ En `main` (PR #3) |
| 2.1 | Usuarios y espacios en la base, migración `V2` (ADR-0028 y 0029). [Prompt 003](docs/etapa-2/prompts/003-paso-2-1-usuarios-y-espacios.md) | ✅ En `main` (PR #4) |
| 2.2 | Login con Google, `GET /api/v1/me` y cierre de sesión (ADR-0030). [Prompt 004](docs/etapa-2/prompts/004-paso-2-2-login-con-google.md) | ✅ En `main` (PR #5). Probado con una cuenta real |
| 2.3 | Elegir y cambiar el espacio (ADR-0031). [Prompt 005](docs/etapa-2/prompts/005-paso-2-3-eleccion-del-espacio.md) | ✅ En `main` (PR #6) |
| 2.4 | Enlaces dentro del espacio, `V3` (ADR-0032). [Prompt 006](docs/etapa-2/prompts/006-paso-2-4-enlaces-en-el-espacio.md) | ✅ En `main` (PR #7). Probado con una cuenta real |
| C.1 | Login con QR desde el celular, `V4` (ADR-0033). [Prompt 007](docs/etapa-2/prompts/007-parte-c-login-con-qr.md) | ✅ En `main` (PR #8). Probado entre dos navegadores de la misma PC; desde el celular falta una URL pública con HTTPS (E9) |
| C.2 | Extensión con sesión y espacio | Implementada (PR #8). Faltan las pruebas manuales en Chrome y Firefox (`extension/README.md`) |
| T | Transición de 60 minutos del espacio anterior, `V5` (ADR-0034). [Prompt 008](docs/etapa-2/prompts/008-transicion-del-espacio.md) | ✅ En `main` (merge local del 2026-11-15, sin push). Probada a mano |
| M | Sección «Mis enlaces», `GET /api/v1/me/links`. [Prompt 009](docs/etapa-2/prompts/009-mis-enlaces.md) | ✅ En `main` (merge local del 2026-11-15, sin push). Probada a mano |
| P | Publicación con HTTPS mediante Tailscale Funnel (ADR-0035). [Prompt 010](docs/etapa-2/prompts/010-publicacion-con-tailscale-funnel.md) | En `feature/etapa2-tailscale-funnel`, sin commit. Falta configurar Google Cloud y probar el login con QR desde el celular |
| QA | QA final y tag `etapa-2` | Pendiente |

Al 2026-11-15, con todo en `main`: **267 tests**, cobertura **98,3 %** de instrucciones y **91,7 %** de ramas.

**Mantener esta tabla actualizada al cerrar cada paso.** El próximo prompt de la Etapa 2 es el 011.

## 10. Pendientes abiertos

- **Docente:** confirmar los supuestos D1 a D8 y E1 a E10. Antes que nada, la E9: dónde se hace la demo. El equipo propone publicar con Tailscale Funnel (ADR-0035).
- **Pruebas manuales** de la extensión en Chrome y Firefox.
- **QA final** de la Etapa 2 y tag `etapa-2`.
- La carpeta `consola-basica/` (versión didáctica en Java puro) no está en git.

## 11. Lecciones técnicas (para no repetir errores)

- **Spring Boot 4.1.1** usa starters modulares: `spring-boot-starter-webmvc`, `-flyway`, `-validation`, `-security-oauth2-client` (el nombre viejo `spring-boot-starter-oauth2-client` está obsoleto) y sus variantes `-test`. Flyway necesita además `org.flywaydb:flyway-database-hsqldb`.
- **springdoc 3.1.1** es la versión compatible con Boot 4.x.
- **`Instant` en Hibernate y HSQLDB:** columna `TIMESTAMP(6)` sin zona horaria. Con `WITH TIME ZONE`, la validación del esquema falla. El reloj de Windows tiene 7 decimales y la base guarda 6: truncar a `ChronoUnit.MICROS`.
- **Reintentos de alias:** cada intento corre en su propia transacción (`TransactionTemplate`). Si no, un choque de `UNIQUE` dejaría la transacción marcada para rollback.
- **Colisiones:** solo se traduce a una excepción del negocio la violación de la restricción UNIQUE esperada (SQLState `23505`). HSQLDB informa el nombre en mayúsculas y a veces con `PUBLIC.`. Con IDENTITY, la inserción duplicada puede fallar ya en `persist`, antes del `flush`.
- **Consultas del Repository:** ejecutarlas dentro de una transacción; los tests directos usan `TransactionTemplate`.
- **Tests:** MockMvc con `MockMvcBuilders.webAppContextSetup(context)`; beans reemplazados con `@MockitoBean`; reloj con `@Import(app.support.TestClockConfig.class)` y `MutableClock`; base limpiada con `JdbcTemplate` en `@BeforeEach`.
- **Errores HTML vs. JSON:** un `@ExceptionHandler` local de un controller tiene prioridad sobre el `@RestControllerAdvice` global. Así la redirección responde la página HTML 404 y la API, ProblemDetail.
- **Una migración Flyway aplicada no se edita.** Cualquier cambio de esquema va en una versión nueva (`V5`, …).
- **Entidades con relaciones fuera de la transacción:** con `open-in-view=false`, una relación `LAZY` falla al usarla en el controller. `EnlaceDeEspacio` carga el espacio de inmediato porque la URL corta necesita su nombre.

## 12. Entorno local

- JDK 25 en `C:\Users\joana\Downloads\jdk25\jdk25`, y `JAVA_HOME` ya apunta ahí. `tests.ps1` también acepta una copia portable en `backend/.gradle/toolchains/jdk-25*/` si existe.
- El **puerto 8080** puede estar ocupado por otro proceso Java ajeno al proyecto: por eso el equipo usa el 8081.
- `gh` (GitHub CLI) está instalado en `C:\Program Files\GitHub CLI\gh.exe`, con la sesión de GitHub iniciada.

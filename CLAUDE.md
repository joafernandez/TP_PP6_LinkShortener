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
| Lenguaje | Java, JDK **25** (LTS, instalado) |
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
| 4 | Redirección, vencimiento, reasignación, respuesta de error | Pendiente |
| 5 | Código QR | Pendiente |
| 6 | Página web | Pendiente |
| 7 | Extensión Chrome/Firefox con pantalla de opciones | Pendiente |
| 8 | QA: cobertura, revisión REST y JPA, contraste con OpenAPI, tag `etapa-1` | Pendiente |

**Mantener esta tabla actualizada al cerrar cada incremento.**

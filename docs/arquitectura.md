# Arquitectura – Link Shortener

Vigente para la Etapa 1. Las decisiones que la justifican están en [docs/adr](adr/README.md).

## 1. Vista general

```
 ┌──────────────┐   ┌──────────────────┐
 │ Página web   │   │ Extensión MV3    │   clientes: sin lógica de negocio
 │ (HTML/JS)    │   │ (Chrome/Firefox) │
 └──────┬───────┘   └────────┬─────────┘
        │  REST /api/v1 (JSON, PNG)
        ▼                    ▼
 ┌─────────────────────────────────────────────┐
 │ Spring Boot                                 │
 │  Controllers ─► Services ─► EntityManager   │──► HSQLDB (database/)
 │   (DTOs)        (reglas)      (JPQL)        │
 └─────────────────────────────────────────────┘
        ▲
        │  GET /{alias}  → 302 a la URL original
 ┌──────┴───────┐
 │ Navegador /  │
 │ lector de QR │
 └──────────────┘
```

- La página web la sirve el mismo Spring Boot, así que comparte origen con la API ([ADR-0013](adr/0013-cliente-web-sin-framework.md)).
- La extensión accede mediante permisos de host, sin CORS ([ADR-0014](adr/0014-extension-mv3-y-permisos-de-host.md)).

## 2. Capas y reglas de dependencia

| Capa | Responsabilidad | Puede usar | No puede |
|---|---|---|---|
| Controller | Traducir HTTP ↔ DTO, validar el formato de entrada, elegir el código HTTP | Services, DTOs | Usar `EntityManager` ni exponer entidades |
| Service | Reglas de negocio y transacciones (`@Transactional`) | Entidades, `EntityManager`, `AliasGenerator`, `Clock`, propiedades | Conocer HTTP ni DTOs de la web |
| Entidad | Estado persistente y comportamiento propio (por ejemplo, saber si está vencida) | — | Depender de Spring o de otras capas |
| Common | Configuración y manejo de errores transversal | — | Contener lógica de negocio |

## 3. Paquetes y clases previstas

```
app/
├── LinkShortenerApplication.java
├── link/
│   ├── Link.java                     entidad
│   ├── LinkService.java              crear, resolver; JPQL en métodos privados
│   ├── UrlValidator.java             criterio de URL válida (ADR-0019, ADR-0020)
│   ├── LinkController.java           POST /api/v1/links
│   ├── RedirectController.java       GET /{alias:[A-Za-z0-9]+}
│   ├── InvalidUrlException.java      URL inválida o del propio servicio → 400
│   ├── AliasUnavailableException.java  sin alias libre tras los reintentos → 503
│   ├── LinkNotFoundException.java    alias inexistente o vencido → 404
│   ├── dto/
│   │   ├── CreateLinkRequest.java
│   │   └── LinkResponse.java
│   └── alias/
│       ├── AliasGenerator.java       interfaz (punto de extensión)
│       └── RandomAliasGenerator.java alfabeto de 56, largo configurable
├── qr/
│   ├── QrCodeService.java            texto → PNG (ZXing)
│   └── QrController.java             GET /api/v1/links/{alias}/qr
└── common/
    ├── config/
    │   ├── AppProperties.java        @ConfigurationProperties("app")
    │   └── ClockConfig.java          bean java.time.Clock
    └── error/
        └── ApiExceptionHandler.java  @RestControllerAdvice → ProblemDetail
```

Los nombres son orientativos y se confirman en cada incremento. `qr` depende de `link` (necesita saber si el alias está vigente). `link` no depende de `qr`.

## 4. Modelo de dominio

### Entidad `Link`, tabla `link`

| Atributo | Tipo Java | Columna | Restricciones |
|---|---|---|---|
| `id` | `Long` | `id` | PK, autogenerada |
| `alias` | `String` | `alias` | `NOT NULL`, `UNIQUE`, largo hasta 16 |
| `originalUrl` | `String` | `original_url` | `NOT NULL`, largo hasta 2048 |
| `createdAt` | `Instant` | `created_at` | `NOT NULL`, `TIMESTAMP(6)` en UTC |
| `expiresAt` | `Instant` | `expires_at` | `NOT NULL`, `TIMESTAMP(6)` en UTC |

- `expiresAt` se guarda al crear o reasignar (`createdAt + ttl`). Así la consulta es simple y cambiar la duración no requiere migrar datos.
- `isExpired(Instant ahora)` → `!ahora.isBefore(expiresAt)`. Un enlace vence exactamente a los 60 minutos.
- El largo de la columna `alias` admite crecer hasta 16 sin migración.
- La tabla se crea con la migración Flyway `V1__create_link.sql` (incremento 3).

## 5. Flujos

### 5.1 Crear un enlace (`POST /api/v1/links`)
1. `LinkController` recibe `CreateLinkRequest { url }`. Bean Validation controla que no esté vacía y que no supere los 2048 caracteres.
2. `LinkService.create(url)`:
   1. `UrlValidator` valida la URL (RN-08) y que no sea del propio servicio (RN-09). Si no cumple, lanza `InvalidUrlException`.
   2. `ahora = clock.instant()` truncado a microsegundos (la precisión de la columna), `vence = ahora + ttl`.
   3. Repite hasta `max-attempts` veces, cada intento en **su propia transacción** (`TransactionTemplate`), para que un choque con otro pedido no invalide los intentos siguientes:
      - `alias = aliasGenerator.generate()` (nunca reservado).
      - Si el alias no existe → `persist` de un nuevo `Link` → fin.
      - Si existe y está **vencido** → actualización atómica `UPDATE Link SET originalUrl, createdAt, expiresAt WHERE alias = :alias AND expiresAt <= :ahora`. Si actualizó una fila → fin.
      - Si existe y está **vigente**, o la base rechazó el alias por `UNIQUE` en una carrera con otro pedido → se intenta con otro alias.
   4. Si se agotan los intentos, lanza una excepción → 503.
3. El controller responde `201` con `LinkResponse` y `Location: {shortUrl}`.

### 5.2 Redirigir (`GET /{alias}`)
1. `RedirectController` recibe un alias alfanumérico.
2. `LinkService.resolve(alias)` busca por alias y verifica que esté vigente. Si no existe o está vencido, lanza `LinkNotFoundException`.
3. Si está vigente, responde `302 Found` con `Location: originalUrl` ([ADR-0012](adr/0012-redireccion-con-302.md)).
4. Si no, responde `404` con una página HTML simple ([ADR-0018](adr/0018-enlace-vencido-o-inexistente.md)).
   - La página está en `resources/pages/link-not-found.html`. No está en `static/` para que no se pueda abrir como página suelta, y no usa un motor de plantillas.
   - La resuelve un `@ExceptionHandler` **local** de `RedirectController`, que tiene prioridad sobre el `ApiExceptionHandler` global. Así la API sigue respondiendo ProblemDetail en JSON y la redirección responde HTML.

### 5.3 Obtener el QR (`GET /api/v1/links/{alias}/qr`)
1. `QrController` le pide a `LinkService` el enlace vigente (404 si no existe o está vencido).
2. `QrCodeService` genera el PNG de la URL corta (`{app.base-url}/{alias}`).
3. Responde `200` con `image/png`.

Detalles del incremento 5 (2026-10-08):
- `QrCodeService.generatePng(String text)` recibe el texto y devuelve un `byte[]` con el PNG, generado en memoria. No consulta enlaces ni conoce HTTP.
- Se usan ZXing `core` y `javase` 3.5.4. La imagen mide 300 × 300 píxeles (constante del servicio), conserva el margen del QR y codifica el texto en UTF-8.
- El controller reutiliza `resolve` y `shortUrlOf`: no duplica la validación de vigencia ni crea un enlace al pedir su QR.
- La respuesta del endpoint incluye `Cache-Control: no-store`, también ante un 404, para que las consultas posteriores vuelvan al servidor y comprueben la vigencia.
- Las pruebas decodifican la imagen para comprobar su contenido. La integración verifica enlace vigente, inexistente, vencimiento exacto, reasignación y consulta sin crear registros ni renovar vigencia, usando el reloj controlable existente.
- El controller documenta explícitamente el PNG, el ProblemDetail y los encabezados para que Swagger publique el contrato del QR correctamente. Las anotaciones de documentación no modifican las reglas de negocio.
- Implementación verificada: 76 tests pasan y se comprobó el JAR por HTTP con una base en memoria, incluida la expiración con una duración temporal de 5 segundos. La configuración normal sigue siendo de 60 minutos.

### 5.4 Vencimiento y reutilización
- **Verificación perezosa:** el vencimiento se evalúa al resolver y al crear, comparando con `clock.instant()`. No hace falta un proceso en segundo plano para que se cumpla la regla.
- Un alias vencido se libera recién cuando el generador lo vuelve a producir, y en ese momento se reutiliza su registro (sin historial, [ADR-0021](adr/0021-sin-historial-de-enlaces.md)).

## 6. Configuración (`app.*`)

| Propiedad | Valor por defecto | Uso |
|---|---|---|
| `app.base-url` | `http://localhost:8080` | Arma la URL corta y detecta las URLs del propio servicio |
| `app.link.ttl` | `60m` | Vigencia de un enlace |
| `app.alias.length` | `5` | Largo del alias |
| `app.alias.alphabet` | `23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz` | Caracteres permitidos |
| `app.alias.max-attempts` | `10` | Reintentos ante una colisión |
| `app.alias.reserved` | `api,error,v3,swagger` | Palabras que nunca se generan |

Todas se pueden sobrescribir con variables de entorno (por ejemplo `APP_BASE_URL`).

## 7. Manejo de errores

| Situación | Dónde | Respuesta |
|---|---|---|
| URL inválida o del propio servicio | API | `400` ProblemDetail (`application/problem+json`) |
| Alias inexistente o vencido | API (QR) | `404` ProblemDetail |
| Alias inexistente o vencido | Redirección | `404` página HTML |
| No se pudo generar un alias libre | API | `503` ProblemDetail |
| Error inesperado | API | `500` ProblemDetail, sin exponer detalles internos |

## 8. Puntos de extensión

Para cambios futuros ([ADR-0010](adr/0010-puntos-de-extension.md)):
- Formato del alias → otra implementación de `AliasGenerator` o cambio de configuración.
- Vigencia, dominio o largo → configuración.
- Contrato de la API → DTOs separados de la entidad, y `/api/v2` si hay cambios incompatibles.
- Esquema → nueva migración Flyway.
- Funcionalidad nueva → paquete hermano bajo `app`.

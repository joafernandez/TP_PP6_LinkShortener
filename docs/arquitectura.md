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
 ┌───────────────────────────────────────────────────────────────────────┐
 │ Spring Boot                                                           │
 │  Controllers ─► Services ─► Repository ─► EntityManager                │──► HSQLDB (database/)
 │   (DTOs)        (reglas y    (persistencia y JPQL)                      │
 │                 transacciones)                                        │
 └───────────────────────────────────────────────────────────────────────┘
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
| Service | Reglas de negocio y transacciones (`TransactionTemplate`, `@Transactional`) | Repository, entidades, `GeneradorAlias`, `Clock`, propiedades | Conocer HTTP, DTOs de la web, JPQL o detalles de errores SQL |
| Repository | Consultas, inserciones, actualización condicional y clasificación de colisiones | Entidades, `EntityManager`, JPQL, excepciones de persistencia | Decidir la vigencia, generar alias o coordinar reintentos |
| Entidad | Estado persistente y comportamiento propio (por ejemplo, saber si está vencida) | — | Depender de Spring o de otras capas |
| Common | Configuración y manejo de errores transversal | — | Contener lógica de negocio |

## 3. Paquetes y clases actuales

```
app/
├── AcortadorEnlacesApplication.java
├── enlace/
│   ├── Enlace.java                    entidad
│   ├── EnlaceService.java             crear, resolver, transacciones y reintentos
│   ├── EnlaceRepository.java          buscar, insertar y reasignar con EntityManager
│   ├── ColisionAliasException.java    colisión confirmada de la restricción del alias
│   ├── ValidadorUrl.java              criterio de URL válida (ADR-0019, ADR-0020)
│   ├── EnlaceController.java          POST /api/v1/links
│   ├── RedireccionController.java     GET /{alias:[A-Za-z0-9]+}
│   ├── UrlInvalidaException.java      URL inválida o del propio servicio → 400
│   ├── AliasNoDisponibleException.java  sin alias libre tras los reintentos → 503
│   ├── EnlaceNoEncontradoException.java  alias inexistente o vencido → 404
│   ├── dto/
│   │   ├── CrearEnlaceRequest.java
│   │   └── EnlaceResponse.java
│   └── alias/
│       ├── GeneradorAlias.java        interfaz (punto de extensión)
│       └── GeneradorAliasAleatorio.java  alfabeto de 56, largo configurable
├── qr/
│   ├── CodigoQrService.java           texto → PNG (ZXing)
│   └── QrController.java              GET /api/v1/links/{alias}/qr
└── common/
    ├── config/
    │   ├── AppProperties.java        @ConfigurationProperties("app")
    │   └── ClockConfig.java          bean java.time.Clock
    └── error/
        └── ApiExceptionHandler.java  @RestControllerAdvice → ProblemDetail
```

El vocabulario del negocio usa nombres en español y conserva sufijos técnicos como `Controller`, `Service`, `Request` y `Response`. `qr` depende de `enlace` (necesita saber si el alias está vigente). `enlace` no depende de `qr`.

## 4. Modelo de dominio

### Entidad `Enlace`, tabla `link`

| Atributo | Tipo Java | Columna | Restricciones |
|---|---|---|---|
| `id` | `Long` | `id` | PK, autogenerada |
| `alias` | `String` | `alias` | `NOT NULL`, `UNIQUE`, largo hasta 16 |
| `urlOriginal` | `String` | `original_url` | `NOT NULL`, largo hasta 2048 |
| `creadoEn` | `Instant` | `created_at` | `NOT NULL`, `TIMESTAMP(6)` en UTC |
| `venceEn` | `Instant` | `expires_at` | `NOT NULL`, `TIMESTAMP(6)` en UTC |

- `venceEn` se guarda al crear o reasignar (`creadoEn + ttl`). Así la consulta es simple y cambiar la duración no requiere migrar datos.
- `estaVencido(Instant ahora)` → `!ahora.isBefore(venceEn)`. Un enlace vence exactamente a los 60 minutos.
- El largo de la columna `alias` admite crecer hasta 16 sin migración.
- La tabla se crea con la migración Flyway `V1__create_link.sql` (incremento 3).

## 5. Flujos

### 5.1 Crear un enlace (`POST /api/v1/links`)
1. `EnlaceController` recibe `CrearEnlaceRequest { url }`. Bean Validation controla que no esté vacía y que no supere los 2048 caracteres.
2. `EnlaceService.crear(url)`:
   1. `ValidadorUrl` valida la URL (RN-08) y que no sea del propio servicio (RN-09). Si no cumple, lanza `UrlInvalidaException`.
   2. `ahora = clock.instant()` truncado a microsegundos (la precisión de la columna), `vence = ahora + ttl`.
   3. Repite hasta `max-attempts` veces, cada intento en **su propia transacción** (`TransactionTemplate`), para que un choque con otro pedido no invalide los intentos siguientes:
      - `alias = generadorAlias.generar()` (nunca reservado).
      - El Repository busca el alias. Si no existe → `insertar` un nuevo `Enlace` (`persist` y `flush`) → fin.
      - Si existe → el Repository intenta una actualización atómica `UPDATE Enlace SET urlOriginal, creadoEn, venceEn WHERE alias = :alias AND venceEn <= :ahora`. Si actualizó una fila, refresca la entidad → fin.
      - Si existe y está **vigente**, o la base rechazó el alias por `UNIQUE` en una carrera con otro pedido → se intenta con otro alias.
      - El Repository traduce a `ColisionAliasException` solamente una cadena de causas que confirme la restricción `uk_link_alias` (o `PUBLIC.UK_LINK_ALIAS`) y el SQLState `23505` de HSQLDB. El service captura esa excepción después del rollback y reintenta. Otros errores se propagan conservando la causa, con la traducción estándar de Spring cuando corresponde; una restricción desconocida tampoco se interpreta como colisión.
   4. Si se agotan los intentos, lanza una excepción → 503.
3. El controller responde `201` con `EnlaceResponse` y `Location: {shortUrl}`.

### 5.2 Redirigir (`GET /{alias}`)
1. `RedireccionController` recibe un alias alfanumérico.
2. `EnlaceService.resolver(alias)` abre una transacción de lectura, consulta al Repository y verifica que esté vigente. Si no existe o está vencido, lanza `EnlaceNoEncontradoException`.
3. Si está vigente, responde `302 Found` con `Location: urlOriginal` ([ADR-0012](adr/0012-redireccion-con-302.md)).
4. Si no, responde `404` con una página HTML simple ([ADR-0018](adr/0018-enlace-vencido-o-inexistente.md)).
   - La página está en `resources/pages/link-not-found.html`. No está en `static/` para que no se pueda abrir como página suelta, y no usa un motor de plantillas.
   - La resuelve un `@ExceptionHandler` **local** de `RedireccionController`, que tiene prioridad sobre el `ApiExceptionHandler` global. Así la API sigue respondiendo ProblemDetail en JSON y la redirección responde HTML.

### 5.3 Obtener el QR (`GET /api/v1/links/{alias}/qr`)
1. `QrController` le pide a `EnlaceService` el enlace vigente (404 si no existe o está vencido).
2. `CodigoQrService` genera el PNG de la URL corta (`{app.base-url}/{alias}`).
3. Responde `200` con `image/png`.

Detalles del incremento 5 (2026-10-08):
- `CodigoQrService.generarPng(String texto)` recibe el texto y devuelve un `byte[]` con el PNG, generado en memoria. No consulta enlaces ni conoce HTTP.
- Se usan ZXing `core` y `javase` 3.5.4. La imagen mide 300 × 300 píxeles (constante del servicio), conserva el margen del QR y codifica el texto en UTF-8.
- El controller reutiliza `resolver` y `urlCortaDe`: no duplica la validación de vigencia ni crea un enlace al pedir su QR.
- La respuesta del endpoint incluye `Cache-Control: no-store`, también ante un 404, para que las consultas posteriores vuelvan al servidor y comprueben la vigencia.
- Las pruebas decodifican la imagen para comprobar su contenido. La integración verifica enlace vigente, inexistente, vencimiento exacto, reasignación y consulta sin crear registros ni renovar vigencia, usando el reloj controlable existente.
- El controller documenta explícitamente el PNG, el ProblemDetail y los encabezados para que Swagger publique el contrato del QR correctamente. Las anotaciones de documentación no modifican las reglas de negocio.
- Implementación verificada: 76 tests pasan y se comprobó el JAR por HTTP con una base en memoria, incluida la expiración con una duración temporal de 5 segundos. La configuración normal sigue siendo de 60 minutos.

### 5.4 Vencimiento y reutilización
- **Verificación perezosa:** el vencimiento se evalúa al resolver y al crear, comparando con `clock.instant()`. No hace falta un proceso en segundo plano para que se cumpla la regla.
- Un alias vencido se libera recién cuando el generador lo vuelve a producir, y en ese momento se reutiliza su registro (sin historial, [ADR-0021](adr/0021-sin-historial-de-enlaces.md)).

### 5.5 Página web (incremento 6)

- `frontend/index.html`, `assets/styles.css` y `assets/app.js` forman una pantalla sin framework, según ADR-0013. `processResources` incorpora esas fuentes a `static/` en el JAR; Spring Boot sirve `index.html` en `/` sin un nuevo controller.
- El formulario envía JSON a `POST /api/v1/links` con `fetch`. Muestra `shortUrl` y `expiresAt` de la respuesta; no fija el TTL ni aplica reglas de creación de alias.
- La imagen consulta `GET /api/v1/links/{alias}/qr` con el alias codificado como segmento. Un fallo de la imagen conserva el enlace y muestra un aviso.
- Las consultas usan rutas relativas. Los mensajes de error usan `ProblemDetail.detail` cuando está disponible y los textos se insertan con `textContent`. El botón se deshabilita mientras se procesa el envío y se recupera también ante errores.
- La interfaz incluye etiquetas, validación nativa de entrada, mensajes de estado, foco visible y adaptación a pantallas pequeñas. El vencimiento se presenta en hora local usando la fecha del servidor.
- `WebResourcesIT` comprueba la ruta de inicio y los recursos empaquetados. Se verificó el JAR por HTTP y el formulario en el navegador: creación, carga de QR, Enter, errores de URL, petición pendiente, falla de QR y pérdida de conexión. La suite del backend tiene 107 tests que pasan; no se presenta la verificación manual del JavaScript como cobertura de JaCoCo.

## 6. Configuración (`app.*`)

| Propiedad | Valor por defecto | Uso |
|---|---|---|
| `app.base-url` | `http://localhost:8080` | Arma la URL corta y detecta las URLs del propio servicio |
| `app.link.ttl` | `60m` | Vigencia de un enlace |
| `app.alias.alphabet` | `123456789abcdefghijkmnpqrstuvwxyz` | Caracteres permitidos, en el orden en que se asignan. Desde la Etapa 2 no hay largo fijo: se usa el menor código libre (ADR-0026) |
| `app.alias.max-attempts` | `10` | Reintentos ante una colisión |
| `app.alias.reserved` | `api,error,v3,swagger` | Palabras que nunca se generan |

Todas se pueden sobrescribir con variables de entorno (por ejemplo `APP_BASE_URL`).

`AppProperties` valida al arrancar, mediante `@Validated` y las restricciones de Jakarta Validation: largo del alias entre 1 y 16, duración estrictamente positiva y URL base con esquema HTTP/HTTPS y host válido. Para las dos últimas comprobaciones se usan métodos `@AssertTrue`; la URL se analiza con `URI`. Una configuración inválida impide crear el contexto de Spring y presenta un mensaje en español con la propiedad afectada. Los valores por defecto se conservan. Las pruebas usan `ApplicationContextRunner` para verificar tanto el arranque válido como el rechazo de valores inválidos, sin abrir la base de datos.

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
- Formato del alias → otra implementación de `GeneradorAlias` o cambio de configuración.
- Vigencia, dominio o largo → configuración.
- Contrato de la API → DTOs separados de la entidad, y `/api/v2` si hay cambios incompatibles.
- Esquema → nueva migración Flyway.
- Funcionalidad nueva → paquete hermano bajo `app`.

## 9. Convención de nombres (2026-10-11)

- El paquete de enlaces es `app.enlace`. Las clases, métodos y atributos del negocio están en español, sin tildes en los identificadores. Se conservan los nombres de Java/Spring y los sufijos técnicos.
- La entidad `Enlace` mantiene `@Table(name = "link")` y las columnas originales mediante `@Column`; las consultas JPQL usan la entidad y los atributos Java traducidos. No se modificó la migración existente.
- `EnlaceResponse` usa `@JsonProperty` para publicar `shortUrl`, `originalUrl`, `createdAt` y `expiresAt`. `@Schema` conserva los nombres públicos `CreateLinkRequest` y `LinkResponse`. Las rutas y los identificadores de operación de la documentación generada se mantienen.
- `AppProperties` usa `@Name` para conservar las claves `app.base-url`, `app.link.*` y `app.alias.*` con accesores Java en español.
- La traducción mantuvo la arquitectura y la ubicación del frontend. Luego se agregó Repository mediante ADR-0024, sin cambiar el contrato público.

## 10. Capa Repository (2026-10-11)

Según [ADR-0024](adr/0024-capa-repository-con-entitymanager.md), se agregó una clase concreta `EnlaceRepository` anotada con `@Repository`. Usa el `EntityManager` existente; no necesita una interfaz propia ni extender `JpaRepository` para las tres operaciones actuales.

- `buscarPorAlias` devuelve `Optional<Enlace>`.
- `insertar` hace `persist` y `flush`, y traduce exclusivamente la colisión confirmada del alias a `ColisionAliasException`, conservando la causa.
- `reasignarSiEstaVencido` hace el UPDATE condicional y refresca la entidad cuando actualiza una fila. La condición se evalúa en la base para impedir que una lectura anterior sobrescriba un alias renovado.

El Repository participa en las transacciones del service. El service conserva validación, generación, cálculo de fechas, decisión de reintentar y coordinación de las transacciones. El SQLState, los nombres de restricciones y las consultas dejan de ser motivos para modificar el service. Crear una clase por caso de uso no es necesario para lograr esta separación.

Las pruebas unitarias del service simulan el Repository; las del Repository comprueban la clasificación de errores. La integración usa HSQLDB real para verificar rollback tras una colisión, inserción en otra transacción, errores ajenos al alias y reasignación condicional ante una lectura anterior.

Verificación final: **125 tests pasan** y el JAR incluye la capa nueva y los recursos web. JaCoCo del backend: **97,20 % de instrucciones**, **96,92 % de líneas** y **93,42 % de ramas**.

# QA de la Etapa 1

- **Fecha:** 2026-10-11.
- **Alcance:** incremento 8 del plan de la etapa (cobertura, revisión REST y JPA, contraste con OpenAPI, cierre con el tag `etapa-1`).
- **Método:** revisión estática del código, de los tests y de la documentación, más las cifras de ejecución que informó el equipo. En la sesión que preparó este informe **no se ejecutaron los tests ni el backend**; las filas marcadas como pendientes se completan con las corridas del equipo.

## 1. Resumen

| Aspecto | Resultado |
|---|---|
| Funcionalidad (criterios de aceptación del backend y la web) | Cubierta por tests automáticos, salvo CA-01.5 (reinicio del servidor), que se verifica a mano |
| Extensión (HU-02 y HU-03) | Solo prueba manual; sin tests automáticos porque el proyecto no usa Node |
| Cobertura | 97,20 % de instrucciones, 96,92 % de líneas y 93,42 % de ramas según el equipo (prompt 010); umbral nuevo en [ADR-0025](../adr/0025-umbral-de-cobertura.md) |
| Contraste con OpenAPI | Un desvío (`operationId`), corregido; una respuesta documentada sin test HTTP (503) |
| REST y JPA | Sin defectos encontrados; observaciones en las secciones 5 y 6 |

## 2. Cobertura de pruebas

| Dato | Valor | Fuente |
|---|---|---|
| Tests | 125, sin fallos ni omitidos | [prompt 011](prompts/011-ajuste-scripts-windows.md), corrida de `tests.ps1` del equipo |
| Instrucciones | 97,20 % | prompt 010 |
| Líneas | 96,92 % | prompt 010 |
| Ramas | 93,42 % | prompt 010 |

**Corrida de este incremento (completar con la salida de `.\tests.ps1`):**

| Dato | Valor |
|---|---|
| Tests ejecutados / fallos / omitidos | _pendiente_ |
| Instrucciones / líneas / ramas | _pendiente_ |
| ¿Pasa la verificación del umbral? | _pendiente_ |

El umbral (90 % instrucciones, 90 % líneas, 85 % ramas) se verifica con `jacocoTestCoverageVerification`; ver el ADR-0025. El reporte está en `backend/build/reports/jacoco/test/html/index.html`.

## 3. Trazabilidad: criterio de aceptación → verificación

Los tests no llevan el identificador del criterio en el nombre, por lo que esta tabla es la que une cada criterio con su evidencia. «Manual» remite a las pruebas de la sección 7 o a las verificaciones registradas en los prompts.

| CA | Verificación automática | Verificación manual |
|---|---|---|
| CA-01.1 | `EnlaceControllerIT.creaUnEnlaceYDevuelve201ConLocation` (formato y alfabeto), `GeneradorAliasAleatorioTest` (largo y alfabeto), `EnlaceServiceIT.armaLaUrlCortaConLaUrlBase` | Web en navegador (prompt 008) |
| CA-01.2 | `ValidadorUrlTest` (vacía, más de 2048, esquema, sin host), `EnlaceControllerIT.rechazaUrlVacia`, `rechazaEsquemaNoPermitidoCon400ProblemDetail`, `rechazaUrlDeMasDe2048Caracteres`, `EnlaceServiceIT.rechazaUrlInvalidaSinGenerarAlias` | Mensaje en pantalla (prompt 008) |
| CA-01.3 | `ValidadorUrlTest.rechazaUrlsDelPropioServicio`, `EnlaceControllerIT.rechazaUrlDelPropioServicio` | Mensaje en pantalla (prompt 008) |
| CA-01.4 | `EnlaceControllerIT.laMismaUrlDosVecesDaAliasDistintos`, `EnlaceServiceIT.laMismaUrlGeneraAliasDistintos` | |
| CA-01.5 | La base es HSQLDB en modo archivo (`application.properties`); los tests usan HSQLDB en memoria | **Sin test automático del reinicio.** Ver el procedimiento en la sección 7 |
| CA-02.1 a CA-02.4 | No hay | Extensión, sección 7 |
| CA-03.1 y CA-03.2 | No hay | Extensión, sección 7 |
| CA-04.1 | `RedireccionControllerIT.redirigeCon302ALaUrlOriginal`, `redirigeHastaUnInstanteAntesDeVencer` | |
| CA-04.2 | `RedireccionControllerIT.enlaceVencidoMuestraPaginaHtml404`, `EnlaceServiceIT.resolverFallaExactamenteALos60Minutos`, `EnlaceTest` (límites de los 60 minutos) | |
| CA-04.3 | `RedireccionControllerIT.aliasInexistenteMuestraLaMismaPaginaHtml404` | |
| CA-04.4 | `RedireccionControllerIT` (`palabraReservada…`, `swaggerUiSigueDisponible`, `especificacionOpenApiSigueDisponible`, `laApiSigueRespondiendoErroresEnJson`), `WebResourcesIT` | |
| CA-05.1 | `EnlaceServiceIT.reasignaUnAliasVencido`, `resolverDevuelveLaNuevaUrlTrasUnaReasignacion`, `QrControllerIT.unAliasReasignado…`, `EnlaceRepositoryIT` (lectura desactualizada) | |
| CA-05.2 | `EnlaceServiceIT.noPisaUnAliasVigenteYGeneraOtro`, `unSegundoAntesDeVencerElAliasSigueOcupado`, `EnlaceServiceTest.pruebaOtroAliasSiLaReasignacionNoFuePosible` | |
| CA-06.1 | `QrControllerIT.devuelveUnPngQueCodificaLaUrlCorta`, `WebResourcesIT` | QR visible en la web (prompt 008) y en la extensión (sección 7) |
| CA-06.2 | `QrControllerIT.devuelveUnPngQueCodificaLaUrlCorta`, `CodigoQrServiceTest.elQrCodificaExactamenteElTextoRecibido` | Escaneo con un celular (sección 7) |
| CA-06.3 | `QrControllerIT.vencidoExactamenteALos60MinutosDevuelve404Json`, `aliasInexistenteDevuelveElMismo404Json` | |

## 4. Contraste con OpenAPI

Se comparó `docs/api/openapi.yaml` con los controllers, los DTOs, el manejador de errores y los tests. La única comprobación automática del documento generado es `QrControllerIT.swaggerDocumentaPng404JsonYCacheComoElContrato`, que cubre el endpoint del QR y los esquemas; el resto se contrastó leyendo el código.

| Elemento del contrato | Implementación | Resultado |
|---|---|---|
| `POST /api/v1/links`, `operationId: createLink` | `@Operation(operationId = "create")` | **Desvío.** Corregido a `createLink` (hallazgo H-01) |
| `201` con `Location` y `LinkResponse` de cinco campos | `EnlaceController.crear`; `EnlaceControllerIT` y el test de esquemas | Coincide |
| `400` con `application/problem+json` | `ApiExceptionHandler` y Bean Validation; `EnlaceControllerIT` (vacía, esquema, propio servicio, 2048, JSON mal formado) | Coincide |
| `503` si no hay alias libre | `ApiExceptionHandler.manejarAliasNoDisponible`; el service se prueba en `EnlaceServiceIT.fallaSiSeAgotanLosIntentos` | Coincide en el código; **sin test HTTP** del 503 (hallazgo H-02) |
| `GET /api/v1/links/{alias}/qr`: `200` PNG con `Cache-Control: no-store` | `QrController.obtenerQr`; `QrControllerIT` | Coincide |
| Mismo endpoint: `404` `problem+json` con `no-store` | `ApiExceptionHandler.manejarEnlaceNoEncontrado`; `QrControllerIT` | Coincide |
| `GET /{alias}`: `302` con `Location` | `RedireccionController.redirigir`; `RedireccionControllerIT` | Coincide |
| `GET /{alias}`: `404` HTML sin distinguir vencido de inexistente | `RedireccionController.manejarNoEncontrado`; `RedireccionControllerIT` | Coincide |
| Patrón del alias `^[A-Za-z0-9]+$` | `/{alias:[A-Za-z0-9]+}` | Coincide |
| `CreateLinkRequest.url`: obligatorio, hasta 2048 | `@NotBlank` y `@Size(max = 2048)`; `ValidadorUrl` | Coincide (`@NotBlank` también rechaza URLs de solo espacios, en línea con RN-08) |

## 5. Revisión de buenas prácticas REST

Conforme al [ADR-0007](../adr/0007-diseno-de-la-api-rest.md):

- Versionado `/api/v1` y recursos en plural (`/api/v1/links`), con el QR como subrecurso (`/links/{alias}/qr`). **Cumple.**
- Códigos de estado correctos: `201` con `Location` al crear, `400` por entrada inválida, `404` por enlace inexistente o vencido, `503` si no hay alias, `302` en la redirección. **Cumple.**
- DTOs de entrada y salida; la entidad `Enlace` no se expone. **Cumple.**
- Errores en `ProblemDetail` (RFC 9457), centralizados en un `@RestControllerAdvice`. **Cumple.**
- Validación con Bean Validation, además de las reglas del `ValidadorUrl`. **Cumple.**
- `GET` sin efectos laterales: el QR no crea enlaces ni renueva la vigencia, y hay un test que lo comprueba. **Cumple.**

Observaciones, sin defectos:

- El `Location` del `201` apunta a la URL corta y no a un recurso de la API. Es lo que dice el contrato, y no existe un `GET /api/v1/links/{alias}` porque el diseño no guarda historial (ADR-0021).
- No hay configuración de CORS. Es deliberado: la web se sirve desde el mismo origen y la extensión usa permisos de host ([ADR-0014](../adr/0014-extension-mv3-y-permisos-de-host.md)).
- No hay autenticación ni límite de pedidos, como indica el contrato (`security: []`). Queda fuera del alcance de la Etapa 1.
- Un acortador redirige a cualquier URL `http` o `https` aceptada. No se verifica que exista (ADR-0020).

## 6. Revisión de persistencia (JPA)

- `Enlace` mapea la tabla `link` y sus columnas con `@Table` y `@Column`; las longitudes (16 y 2048) y la nulabilidad coinciden con `V1__create_link.sql`. `ddl-auto=validate` hace que Hibernate compruebe esa coincidencia al arrancar. **Cumple.**
- El esquema lo crea Flyway, no Hibernate ([ADR-0005](../adr/0005-migraciones-con-flyway.md)), y la restricción `UNIQUE` del alias existe en la base. **Cumple.**
- Las fechas son `Instant` sobre `TIMESTAMP(6)`, y el service trunca a microsegundos para que lo devuelto coincida con lo persistido. **Cumple.**
- Cada intento de crear un alias corre en su propia transacción (`TransactionTemplate`), y el repositorio traduce solo la violación de `uk_link_alias` a `ColisionAliasException`, que permite reintentar. Otros errores se propagan. Tienen test unitario y de integración con HSQLDB real. **Cumple.**
- La reasignación de un alias vencido usa un `UPDATE` condicionado a que siga vencido y refresca la entidad después, porque un `UPDATE` JPQL no actualiza el contexto de persistencia. **Cumple**, con test de lectura desactualizada (`EnlaceRepositoryIT`).
- `resolver` es `@Transactional(readOnly = true)` y `open-in-view` está desactivado. **Cumple.**
- La consulta por alias aprovecha el índice único. No hace falta un índice por `expires_at` con las consultas actuales.
- Observación: `Enlace` no define `equals` ni `hashCode`. No se usa en colecciones ni comparaciones, por lo que no es un problema hoy.

## 7. Prueba manual

### Extensión (HU-02 y HU-03), en cada navegador

Procedimiento en [extension/README.md](../../extension/README.md). Completar con el resultado de cada caso.

| # | Caso | CA | Chrome | Firefox |
|---|---|---|---|---|
| 1 | Sin configurar: mensaje y botón «Abrir opciones» | CA-02.3 | OK (visto en el popup) | _pendiente_ |
| 2 | Guardar una dirección vacía o sin esquema: error y no se guarda | CA-03.1 | _pendiente_ | _pendiente_ |
| 3 | Rechazar el permiso: mensaje y no se guarda | CA-03.2 | _pendiente_ | _pendiente_ |
| 4 | Aceptar el permiso: «Guardado» | CA-03.1 | _pendiente_ | _pendiente_ |
| 5 | Página `https://`: URL corta, vencimiento y QR | CA-02.1, CA-06.1 | _pendiente_ | _pendiente_ |
| 6 | Abrir la URL corta: redirige a la original | CA-04.1 | _pendiente_ | _pendiente_ |
| 7 | Escanear el QR con el celular (red compartida y `APP_BASE_URL` con la IP de la PC) | CA-06.2 | _pendiente_ | _pendiente_ |
| 8 | Página no acortable (`chrome://` o `about:`): error | CA-02.2 | _pendiente_ | _pendiente_ |
| 9 | Backend apagado: mensaje de conexión | CA-02.3 | _pendiente_ | _pendiente_ |
| 10 | Permiso revocado: mensaje y botón de opciones | CA-02.3 | _pendiente_ | _pendiente_ |
| 11 | Estando en una URL corta del propio servicio: error del backend | RN-09 | _pendiente_ | _pendiente_ |
| 12 | Mismo código en los dos navegadores | CA-02.4 | _pendiente_ | _pendiente_ |

### Persistencia tras reiniciar (CA-01.5)

1. Levantar el backend y acortar una URL desde la web. Anotar la URL corta.
2. Detener el servidor con `Ctrl + C` y volver a levantarlo con `bootRun`, dentro de los 60 minutos.
3. Abrir la URL corta. Debe redirigir a la URL original.

Resultado: _pendiente_.

## 8. Hallazgos

| ID | Hallazgo | Estado |
|---|---|---|
| H-01 | El `operationId` generado para crear un enlace era `create` y el contrato dice `createLink`. El código ya lo generaba así antes del cambio de nombres a español | **Corregido** en `EnlaceController` y en `QrControllerIT`, que fijaba el valor anterior |
| H-02 | El `503` documentado no tiene test HTTP; solo se prueba el service | Abierto: se propone un test de integración que fuerce el agotamiento de intentos |
| H-03 | CA-01.5 (persistencia tras reiniciar) no tiene test automático | Abierto: se cubre con la verificación manual de la sección 7 |
| H-04 | La extensión no tiene tests automáticos (el proyecto no usa Node) | Aceptado: se cubre con la prueba manual de la sección 7 |
| H-05 | Los tests no nombran el criterio de aceptación que verifican | Mitigado con la tabla de la sección 3 |
| H-06 | `docs/arquitectura.md` (sección 9) dice que los identificadores de operación se mantienen; tras H-01 el de crear cambia | Abierto: corregir esa oración |

## 9. Cierre de la etapa

Con las secciones 2 y 7 completas y el estado de H-02 y H-06 decidido:

1. Desde `backend/`, ejecutar `.\tests.ps1` y confirmar que pasan todos los tests y la verificación del umbral.
2. Guardar y subir los cambios con Git.
3. Crear el tag sobre el commit de cierre y subirlo: `git tag etapa-1` y `git push origin etapa-1`.

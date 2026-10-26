# 002 – Parte A: códigos secuenciales para usuarios no logueados

- **Fecha:** 2026-10-25
- **Herramienta:** Claude Code (VS Code)
- **Objetivo:** implementar el Camino 1 de la Etapa 2 (incrementos A.2 y A.3) sobre la arquitectura existente, sin romper lo de la Etapa 1.
- **Estado:** implementado y verificado. Sin commit. Falta la prueba manual de la extensión.

## Prompt (resumido)

Tras aprobar la documentación del incremento A.1 ([prompt 001](001-analisis-entrega-2.md)), el equipo pidió: *"hacé lo que corresponda para llegar al objetivo final sin romper nada y dejá anotado lo que hacés, lo recomendado"*. Con eso aprobó las dos recomendaciones pendientes: eliminar el generador aleatorio y quitar `app.alias.length`.

## Diseño antes del código

- Contract-first (ADR-0008): primero se actualizó `docs/api/openapi.yaml` (regla del menor alias libre y ejemplos `1`) y la tabla de configuración de `docs/arquitectura.md`. El contrato de los endpoints no cambia.
- `GeneradorAlias.generar(Set<String> ocupados)`: el generador recibe los alias vigentes y no accede a la base (ADR-0027).
- `GeneradorCodigoSecuencial`: recorre los códigos en orden shortlex como un odómetro y devuelve el primero que no está ocupado ni reservado. Termina siempre, porque los ocupados son finitos.
- `EnlaceRepository.buscarAliasVigentes(ahora)`: JPQL con parámetro nombrado (`venceEn > :ahora`), la misma regla que `Enlace.estaVencido`.
- `EnlaceService`: la lectura de vigentes y la generación pasan **dentro** de la transacción de cada intento. Si hay una colisión, el intento siguiente vuelve a leer los vigentes y elige el siguiente libre. Se conservan la reasignación atómica de vencidos (ADR-0021), los reintentos y la clasificación de colisiones.
- Sin cambios de base de datos, API, web ni extensión. Los enlaces de la Etapa 1 (alias de 5 caracteres) siguen funcionando hasta vencer y no interfieren con la secuencia.

## Cambios

| Tipo | Archivos |
|---|---|
| Nuevos | `alias/GeneradorCodigoSecuencial.java`, `GeneradorCodigoSecuencialTest` (7 tests), `CodigosSecuencialesIT` (7 tests, uno por criterio) |
| Eliminados | `alias/GeneradorAliasAleatorio.java` y su test: ya no cumplen el requerimiento; quedan en el historial de git |
| Modificados (código) | `GeneradorAlias`, `EnlaceRepository`, `EnlaceService`, `AppProperties` (sin `longitud`), `application.properties` de main y test (sin `app.alias.length`, alfabeto nuevo), ejemplo de `QrController` |
| Modificados (tests) | `EnlaceServiceTest` (+1 test del orden dentro de la transacción), `EnlaceRepositoryIT` (+1 test de vigentes), `AppPropertiesTest` (se quitan los límites del largo y se comprueba que una configuración vieja con `app.alias.length` sigue arrancando), y ajustes mecánicos de `generar()` a `generar(any())` en `EnlaceServiceIT` y `QrControllerIT`, y del patrón de alias en `EnlaceControllerIT` |
| Documentación | `openapi.yaml`, `arquitectura.md`, `extension/README.md` (caso de prueba 5), `CLAUDE.md` |

## Criterios de aceptación → tests (`CodigosSecuencialesIT`)

| Criterio | Test |
|---|---|
| 1. Código de 1 carácter si hay disponible | `ca1_unaUrlValidaRecibeUnCodigoDeUnCaracter` |
| 2. Códigos incrementales sin repetirse | `ca2_urlsConsecutivasRecibenCodigosIncrementalesSinRepetirse` |
| 3. Un código expirado vuelve a estar disponible | `ca3_elCodigoDeUnEnlaceExpiradoVuelveAQuedarDisponible` |
| 4. Al agotarse, sigue con 2 caracteres | `ca4_alAgotarseLosDeUnCaracterContinuaConLosDeDos` |
| 5. Un código vigente redirige | `ca5_unCodigoVigenteRedirigeALaUrlOriginal` |
| 6. Un código expirado deja de redireccionar | `ca6_unCodigoExpiradoDejaDeRedireccionar` |
| 7. Un código reutilizado redirige solo a la nueva URL | `ca7_unCodigoReutilizadoRedirigeSoloALaNuevaUrl` |

## Verificación

- `gradlew clean test --offline --no-daemon`: **132 tests**, sin fallos ni omitidos. JaCoCo: **97,36 % de instrucciones**, **97,09 % de líneas**, **94,05 % de ramas**, por encima del umbral de ADR-0025.
- Primera corrida: el contexto de Spring no arrancaba porque `GeneradorCodigoSecuencial` tiene dos constructores (uno para tests) y Spring no sabía cuál usar. Se corrigió con `@Autowired` en el público, igual que tenía el generador anterior.
- Servidor real en el puerto 8081 con la base `database/` existente: tres pedidos devolvieron `/1`, `/2` y `/3`; `/1` redirigió con 302, su QR respondió PNG y la página web 200. Un enlace viejo de 5 caracteres respondió 404 porque ya había vencido.

## Pendiente

- Prueba manual de la extensión (no cambió su código).
- Commit, cuando lo pida el equipo.
- Parte B: requiere aprobar Spring Security y el modelo de datos, y las respuestas E3 a E9 de la minuta 02.

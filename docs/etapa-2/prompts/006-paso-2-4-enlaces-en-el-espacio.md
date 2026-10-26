# 006 – Paso 2.4: enlaces dentro del espacio

- **Fecha:** 2026-10-25
- **Herramienta:** Claude Code (VS Code)
- **Objetivo:** que el usuario con sesión acorte dentro de su espacio (`/joana/1`, `/joana/final`) y que, al cambiar de espacio, los enlaces del anterior dejen de funcionar.
- **Estado:** implementado y verificado con tests y con el servidor real. Sin commit.

## Prompt (resumido)

Tras probar el paso 2.3, el equipo notó que el enlace seguía saliendo anónimo (`/7`). Se explicó que era el paso 2.4, y el equipo pidió: «hacelo ya de una los 2 pasos», es decir, guardar el 2.3 en git (PR #6) e implementar el 2.4 sin pasar antes por la aprobación de la lista de archivos. La lista se informa en este registro.

## Diseño antes del código

- Contract-first: `POST /api/v1/me/links` (`CreateNamespaceLinkRequest`), `GET /{namespace}/{code}` y `GET /api/v1/namespaces/{namespace}/links/{code}/qr`.
- Tabla propia `namespace_link` (V3), para no tocar el camino anónimo (ADR-0032).
- Se reutilizan `ValidadorUrl`, `GeneradorAlias`, la reasignación de vencidos y el patrón de una transacción por intento.
- Cambio de espacio: se vencen los enlaces vigentes en la misma transacción del renombre (ADR-0031).

## Cambios

| Tipo | Archivos |
|---|---|
| Base de datos | `V3__create_namespace_link.sql` |
| Código nuevo | `enlace/EnlaceDeEspacio`, `EnlaceDeEspacioRepository`, `EnlaceDeEspacioService`, `EnlaceDeEspacioController`, `ValidadorCodigo`, `CodigoInvalidoException`, `CodigoOcupadoException`, `EspacioNoElegidoException`, `dto/CrearEnlaceDeEspacioRequest`; `common/persistencia/RestriccionUnica` |
| Código modificado | `RedireccionController` (ruta `/{espacio}/{código}`), `QrController` (QR de espacio; rutas completas en cada método), `EspacioService` (vence los enlaces al renombrar), `EnlaceRepository` y `EspacioRepository` (usan `RestriccionUnica`), `ApiExceptionHandler` (400 y 409) |
| Web | `index.html`, `assets/app.js`, `assets/styles.css`: con espacio, alta en el espacio con código opcional y QR del espacio; con sesión sin espacio, pide elegirlo |
| Tests | `EnlacesDeEspacioIT` (17), `ValidadorCodigoTest` (15 casos), `EspacioServiceTest` (verifica que el renombre vence los enlaces) |
| Documentación | `openapi.yaml`, ADR-0032, `CLAUDE.md` |

## Verificación

- `gradlew test`: **227 tests**, sin fallos, en la primera corrida. JaCoCo: **98,07 % de instrucciones**, **97,65 % de líneas**, **94,03 % de ramas**.
- `EnlacesDeEspacioIT` cubre: códigos incrementales en el espacio; el mismo código en dos espacios; independencia del camino anónimo; códigos personalizados con `-` y `_`; el automático saltea los personalizados; código ocupado (409, aunque cambien las mayúsculas); código vencido reutilizado que lleva solo a la URL nueva; código inválido (400); vencimiento exacto a los 60 minutos; mayúsculas en la URL; cambio de espacio (los enlaces viejos dan 404 con el nombre viejo y con el nuevo, y los códigos quedan libres); otro usuario que toma el nombre liberado no hereda enlaces; sin espacio (409); sin sesión (401); sin CSRF (403); URL inválida; QR (PNG y 404); colisión real en la base traducida a `ColisionAliasException`.
- Servidor real con las credenciales de Google: Flyway aplicó la `V3` sobre la base existente; web 200 con el campo nuevo; alta anónima 201; `/nadie/1` 404; `/v3/api-docs` 200.

## Pendiente

- Prueba manual en la web con la cuenta real.
- Etapa 3: la extensión acorta en el espacio si hay sesión, y login con QR.

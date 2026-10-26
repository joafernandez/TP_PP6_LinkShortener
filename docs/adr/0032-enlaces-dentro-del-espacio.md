# ADR-0032 – Enlaces dentro del espacio del usuario

- **Estado:** Aceptada
- **Fecha:** 2026-10-25
- **Etapa:** 2
- **Origen:** requerimiento del Cliente para la Etapa 2 (Camino 2) y criterios aprobados por el equipo ([minuta 02](../etapa-2/minutas/minuta-02-elucidacion.md))

## Contexto
Con sesión, el usuario acorta dentro de su espacio: `notes.io/melina/1`. Sin código personalizado, el código es incremental dentro del espacio; con código (`final`, `final-2026`, `tp_java`), se usa si está libre en el espacio. Los códigos solo son únicos dentro de cada espacio (`ana/1` y `melina/1` conviven). Vigencia de 60 minutos; un código vencido vuelve a estar disponible. Al cambiar de espacio, los enlaces del anterior dejan de funcionar.

## Decisión
- **Tabla propia `namespace_link`** (migración `V3`), con `UNIQUE (namespace_id, alias)`, un `CHECK` de caracteres y `VARCHAR(15)`. La tabla `link` y su `UNIQUE (alias)` global no cambian: el camino anónimo queda intacto.
- **Mismas reglas que el camino anónimo**, en `EnlaceDeEspacioService`: `ValidadorUrl`; el mismo `GeneradorAlias` (menor código libre, ADR-0026), que recibe los códigos vigentes **del espacio**; reasignación atómica de vencidos (ADR-0021); una transacción por intento con reintento ante colisión.
- **Código personalizado:** `ValidadorCodigo` exige minúsculas, números, `-` y `_` (requerimiento del Cliente) y hasta 15 caracteres (ADR-0029). Las mayúsculas se tratan como minúsculas, igual que en el espacio (decisión del equipo). Un código de un enlace vigente del espacio se rechaza con 409; uno vencido se reasigna.
- **API:** `POST /api/v1/me/links` con `{url, code?}` → 201 con `Location` y el mismo `LinkResponse`. Errores: 400 (URL o código inválidos), 401 (sin sesión), 403 (sin token CSRF) y 409 (código ocupado o espacio sin elegir).
- **Redirección:** `GET /{espacio}/{código}` → 302, o la misma página 404 del camino anónimo. Espacio y código se tratan en minúsculas (`/Melina/1` = `/melina/1`). Los segmentos no admiten puntos (los recursos estáticos no chocan), y las rutas literales como `/v3/api-docs` tienen prioridad sobre el patrón.
- **QR:** `GET /api/v1/namespaces/{espacio}/links/{código}/qr`, con las mismas reglas que el QR anónimo.
- **Cambio de espacio** ([ADR-0031](0031-eleccion-y-cambio-del-espacio.md)): en la misma transacción del renombre, `vencerVigentes` pone `expires_at = ahora` a los enlaces vigentes del espacio. Dejan de redirigir con el nombre viejo y con el nuevo, y sus códigos quedan libres.
- La detección de colisiones UNIQUE se extrae a `app.common.persistencia.RestriccionUnica`, porque ya la usaban tres Repository (lo anticipaba ADR-0028).

## Alternativas consideradas
- **Una sola tabla `link` con `namespace_id`:** obliga a representar el espacio anónimo (una fila especial en `namespace` o `NULL`). Con `NULL`, `UNIQUE (namespace_id, alias)` deja de impedir alias anónimos repetidos; con una fila especial hay que relajar las restricciones de `namespace` que pidió el Cliente. Además, la migración tocaría datos existentes.
- **Borrar los enlaces al cambiar de espacio:** también los apaga, pero vencerlos conserva el mismo mecanismo de reutilización que el resto del sistema (ADR-0021).
- **Rechazar las mayúsculas del código personalizado:** más estricto, pero incoherente con el espacio, que el Cliente pidió tratar en minúsculas.

## Consecuencias
- `EnlaceDeEspacio` y `EnlaceDeEspacioRepository` repiten la forma de `Enlace` y `EnlaceRepository`. Se acepta para no arriesgar el camino anónimo; si aparece una tercera clase de enlace, conviene una base común.
- `EspacioService` usa `EnlaceDeEspacioRepository` y `EnlaceDeEspacio` referencia a `Espacio`: los paquetes `enlace` y `espacio` dependen entre sí.
- La extensión sigue usando el camino anónimo; acortar en el espacio desde la extensión queda para la Etapa 3.

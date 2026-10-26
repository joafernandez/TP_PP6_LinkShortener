# 005 – Paso 2.3: elegir y cambiar el espacio de nombres

- **Fecha:** 2026-10-25
- **Herramienta:** Claude Code (VS Code)
- **Objetivo:** que el usuario con sesión elija su espacio de nombres y pueda cambiarlo, con mensajes claros cuando el nombre está ocupado o es inválido.
- **Estado:** implementado y verificado con tests y con el servidor real. Sin commit.

## Prompt (resumido)

Tras hacer el merge del PR #5 (paso 2.2), el equipo aprobó con «sí» la lista de archivos del paso 2.3.

## Diseño antes del código

- Contract-first: `PUT /api/v1/me/namespace` con `NamespaceRequest` y `NamespaceResponse`, y las respuestas 200, 400, 401, 403 y 409.
- `ValidadorEspacio`: minúsculas y números (requerimiento del Cliente); hasta 15 caracteres y nombres reservados configurables (decisiones técnicas, ADR-0029).
- `EspacioService.elegir`: crea la primera vez y después renombra la misma fila. La disponibilidad la decide el `UNIQUE` de la base (409).
- `EspacioRepository.guardarCambioDeNombre` reutiliza la traducción de errores de `insertar`.
- Web: sección «Tu espacio», visible con sesión, con la advertencia de que cambiar el espacio apaga los enlaces del anterior.

## Cambios

| Tipo | Archivos |
|---|---|
| Código | `espacio/ValidadorEspacio`, `EspacioService`, `EspacioController`, `NombreEspacioInvalidoException`, `dto/EspacioRequest`, `dto/EspacioResponse`; `Espacio.renombrar` y `LARGO_MAXIMO`; `EspacioRepository.guardarCambioDeNombre`; mensaje de `EspacioOcupadoException`; manejadores 400 y 409 en `ApiExceptionHandler` |
| Configuración | `AppProperties.EspacioSettings` (`app.namespace.reserved`) en main y test. `EnlaceServiceTest` y `ValidadorUrlTest` construyen `AppProperties` con el componente nuevo en `null` |
| Web | `index.html`, `assets/app.js`, `assets/styles.css` |
| Tests | `ValidadorEspacioTest` (22 casos), `EspacioServiceTest` (5), `EspacioControllerIT` (6) |
| Documentación | `openapi.yaml`, ADR-0031, `CLAUDE.md` |

## Verificación

- `gradlew test`: **195 tests**, sin fallos. JaCoCo: **97,88 % de instrucciones**, **97,85 % de líneas**, **92,37 % de ramas**.
- `EspacioControllerIT`, con la seguridad real: elegir en minúsculas y verlo en `/api/v1/me`; 409 con mensaje si otro usuario lo tiene (aunque cambien las mayúsculas); al cambiarlo, otro usuario puede tomar el anterior; 400 con un mensaje distinto para caracteres, largo y reservados; 401 sin sesión; 403 sin token CSRF.
- Servidor real con las credenciales de Google: web 200 con la sección nueva y `PUT /api/v1/me/namespace` sin sesión → 401.

## Pendiente

- Al cambiar el espacio, vencer los enlaces del anterior: se hace en el paso 2.4, cuando existan enlaces dentro del espacio (ADR-0031).

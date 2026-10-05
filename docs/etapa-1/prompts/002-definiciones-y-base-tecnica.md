# 002 – Definiciones, base técnica y diseño

- **Fecha:** 2026-10-05
- **Herramienta:** Claude Code (VS Code)
- **Objetivo:** cerrar las decisiones técnicas, armar la base del proyecto y documentar el diseño de la Etapa 1.

## Secuencia de prompts (resumida)

| # | Prompt del equipo (resumen) | Resultado |
|---|---|---|
| 1 | Verificar si las respuestas del equipo a D1–D10 coinciden con la consigna | La IA confirmó que coincidían, señaló que D4 y D5 también estaban indefinidas y recordó que la consigna exige OpenAPI y tests |
| 2 | Pedir una propuesta completa: primero la consigna, después las decisiones del equipo con su recomendación, y una estructura preparada para crecer | Plan en tres capas: exigencias de la consigna, decisiones D1–D20 con recomendación, y estructura, prácticas e incrementos |
| 3 | Corregir el plan: CORS de la extensión y la ruta `/{alias}` | Se adoptaron `optional_host_permissions` y una ruta alfanumérica con palabras reservadas |
| 4 | Aprobar el plan, con un agregado: `permissions.request()` va en el handler del clic, antes de cualquier `await` | Se generaron la minuta 01 y la base técnica (Spring Boot 4.1.1, Gradle Wrapper, HSQLDB, test de arranque) |
| 5 | Pedir un `CLAUDE.md` para documentar y centralizar todo | `CLAUDE.md` con contexto, reglas, stack, arquitectura, convenciones y plan |
| 6 | Pedir los ADR de las decisiones técnicas | ADR-0001 a ADR-0015 |
| 7 | Pedir la recomendación para cada duda de la minuta y adoptarlas como supuestos | ADR-0016 a ADR-0023, marcados como pendientes de confirmación del Cliente |
| 8 | Completar el diseño del incremento 1 | `requerimientos.md`, `arquitectura.md` y `openapi.yaml` |

## Correcciones del equipo sobre las propuestas de la IA

- **CORS en la extensión:** la IA propuso habilitar CORS solo para el origen de la extensión. El equipo lo corrigió: en Firefox ese origen es un UUID distinto en cada instalación. Se resuelve con permisos de host (ADR-0014).
- **Ruta `/{alias}` en la raíz:** el equipo advirtió que choca con la web y con Swagger. Se restringió a un segmento alfanumérico y se agregó una lista de palabras reservadas (ADR-0011).
- **`permissions.request()`:** debe llamarse directamente en el handler del clic, antes de cualquier `await`, porque si no Firefox lo rechaza (ADR-0014).

## Verificación

- `gradlew test`: BUILD SUCCESSFUL.
- `gradlew bootRun`: la aplicación arranca y crea la base de datos en `database/`.

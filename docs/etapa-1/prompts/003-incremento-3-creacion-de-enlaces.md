# 003 – Incremento 3: creación de enlaces

- **Fecha:** 2026-10-05
- **Herramienta:** Claude Code (VS Code)
- **Objetivo:** implementar `POST /api/v1/links` según el contrato OpenAPI y los ADR 0016 a 0021.

## Secuencia de prompts (resumida)

| # | Prompt del equipo (resumen) | Resultado |
|---|---|---|
| 1 | "¿El incremento 3 ya está preparado?" | La IA presentó la lista de archivos, dependencias y tests, y esperó aprobación |
| 2 | "Sí" (aprobación) | Implementación completa con tests |
| 3 | "Seguimos después, anotá todo en CLAUDE.md" | Estado, próximo paso y lecciones técnicas registradas en `CLAUDE.md` |

## Resultado

- Entidad `Link`, migración `V1`, `RandomAliasGenerator`, `UrlValidator`, `LinkService`, `LinkController`, `ApiExceptionHandler`, `AppProperties` y `ClockConfig`.
- 54 tests (unitarios y de integración), con 95% de cobertura.
- Verificación manual con `curl`: 201 con `Location`, 400 con ProblemDetail y Swagger UI disponible.

## Problemas detectados durante la implementación

- **Tipo de columna incorrecto:** la migración usaba `TIMESTAMP WITH TIME ZONE`, pero Hibernate mapea `Instant` a `TIMESTAMP` normalizado a UTC. La validación del esquema (`ddl-auto=validate`) lo detectó en los tests.
- **Precisión del reloj:** en Windows, `Instant` tiene 7 decimales y la columna guarda 6. Se trunca a microsegundos.
- **Ajuste al contrato:** se quitó `additionalProperties: false` del `openapi.yaml`, porque Spring ignora los campos desconocidos y el contrato no debía prometer un rechazo que no ocurre.

## Decisiones de implementación no previstas en el diseño

- Validación de URL en una clase propia (`UrlValidator`), para poder testearla sin `EntityManager`.
- Una transacción por intento de alias (`TransactionTemplate`), para que un choque de `UNIQUE` con un pedido concurrente no invalide los reintentos.

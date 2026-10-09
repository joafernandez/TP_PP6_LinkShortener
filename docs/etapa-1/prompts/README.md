# Prompts – Etapa 1

Registro de los prompts relevantes usados con la IA (Claude Code y Codex) durante la Etapa 1. La consigna (§3) pide documentar las especificaciones y los prompts de ingeniería usados antes de generar código.

## Convención

- Un archivo por prompt o sesión relevante: `NNN-titulo.md`.
- Cada archivo incluye: objetivo, prompt (textual o resumido), resultado obtenido y qué se corrigió o decidió a partir del resultado.
- No hace falta registrar las consultas menores (dudas puntuales o explicaciones).

## Índice

| # | Prompt | Resultado |
|---|---|---|
| [001](001-prompt-inicial.md) | Prompt inicial: contexto, lineamientos y análisis de la Etapa 1 | Análisis de requerimientos, arquitectura propuesta, dudas D1 a D8 |
| [002](002-definiciones-y-base-tecnica.md) | Revisión de decisiones, plan completo, base técnica, ADRs y diseño | Minuta, proyecto base, CLAUDE.md, ADRs 0001 a 0023, requerimientos, arquitectura y OpenAPI |
| [003](003-incremento-3-creacion-de-enlaces.md) | Incremento 3: creación de enlaces | `POST /api/v1/links`, 54 tests, 95% de cobertura |
| [004](004-incremento-4-redireccion.md) | Incremento 4: redirección | `GET /{alias}` con 302 o página 404, 66 tests, 95% de cobertura |
| [005](005-incremento-5-codigo-qr.md) | Incremento 5: código QR, explicado por pasos | Servicio y endpoint QR, 404 JSON, 76 tests pasan y comprobación HTTP del JAR |
| [006](006-reintentos-por-colision-de-alias.md) | Corrección del manejo de errores de persistencia | Colisión real comprobada en HSQLDB, otros errores propagados y 89 tests pasan; guardado en Git a pedido del equipo, sin push |
| [007](007-validacion-basica-de-configuracion.md) | Validación básica de configuración al arrancar | Tres comprobaciones, rechazo de configuración inválida y 105 tests pasan; guardado en Git a pedido del equipo, sin push |

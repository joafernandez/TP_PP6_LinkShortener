# Prompts – Etapa 2

Registro de los prompts relevantes usados con la IA durante la Etapa 2. Sigue la misma convención que el [registro de la Etapa 1](../../etapa-1/prompts/README.md).

## Convención

- Un archivo por prompt o sesión relevante: `NNN-titulo.md`.
- Cada archivo incluye: objetivo, prompt (textual o resumido), resultado obtenido y qué se corrigió o decidió a partir del resultado.
- No hace falta registrar las consultas menores (dudas puntuales o explicaciones).

## Índice

| # | Prompt | Resultado |
|---|---|---|
| [001](001-analisis-entrega-2.md) | Análisis de impacto de los requerimientos de la Etapa 2 | Plan en 3 partes, alternativas de autenticación, minuta 02 y ADRs 0026 y 0027 |
| [002](002-parte-a-codigos-secuenciales.md) | Parte A: códigos secuenciales para anónimos | `GeneradorCodigoSecuencial`, un test por criterio, 132 tests y verificación con el servidor real |
| [003](003-paso-2-1-usuarios-y-espacios.md) | Paso 2.1: usuarios y espacios en la base | Migración V2, unicidad garantizada por la base, máximo técnico de 15 caracteres, 147 tests |
| [004](004-paso-2-2-login-con-google.md) | Paso 2.2: login con Google | Spring Security con oauth2Login, `/api/v1/me`, CSRF en modo spa, 163 tests |
| [005](005-paso-2-3-eleccion-del-espacio.md) | Paso 2.3: elegir y cambiar el espacio | `PUT /api/v1/me/namespace`, validación con mensajes claros, 195 tests |

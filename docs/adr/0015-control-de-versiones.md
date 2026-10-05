# ADR-0015 – Control de versiones con git y un tag por etapa

- **Estado:** Aceptada
- **Fecha:** 2026-10-05
- **Etapa:** 1
- **Origen:** decisión del equipo

## Contexto
La consigna evalúa cómo evoluciona el sistema a lo largo de tres etapas y si el diseño soporta los cambios sin reescrituras masivas. Además, el trabajo es grupal.

## Decisión
- Repositorio **git** desde el inicio del proyecto.
- **Un commit por incremento**, con un mensaje que lo describa.
- **Un tag al cerrar cada etapa** (`etapa-1`, `etapa-2`, `etapa-3`).
- No se versionan la base de datos local (`database/`) ni los archivos de compilación (`build/`, `.gradle/`).
- Más adelante se puede publicar el repositorio en un remoto (por ejemplo GitHub) para compartirlo con el grupo.

## Alternativas consideradas
- **Sin control de versiones o compartiendo archivos comprimidos:** no deja historial ni permite comparar etapas o volver atrás.

## Consecuencias
- Comparar los tags muestra exactamente qué cambió de una etapa a la otra, lo que sirve de evidencia de tolerancia al cambio.
- Cualquier incremento se puede revertir si algo se rompe.

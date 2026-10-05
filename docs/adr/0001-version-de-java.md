# ADR-0001 – Java 25 como versión del JDK

- **Estado:** Aceptada
- **Fecha:** 2026-10-05
- **Etapa:** 1
- **Origen:** decisión del equipo

## Contexto
La consigna exige Java pero no fija una versión. En el entorno de desarrollo está instalado el JDK 25, y la versión elegida debe ser compatible con la de Spring Boot.

## Decisión
Usar **JDK 25**, configurado como *toolchain* de Gradle (`JavaLanguageVersion.of(25)`).

## Alternativas consideradas
- **JDK 21:** también es LTS y compatible, pero habría que configurarlo como JDK del proyecto. No aporta ventajas para el alcance del TP.
- **JDK 17:** LTS más antiguo, sin ventajas para un proyecto nuevo.

## Consecuencias
- Es una versión LTS reciente, soportada por Spring Boot 4.x.
- Cada integrante del grupo necesita un JDK 25 instalado. Está indicado en el README.
- Cambiar de versión más adelante solo requiere modificar el toolchain en `build.gradle`.

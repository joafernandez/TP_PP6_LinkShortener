# ADR-0002 – Spring Boot 4.1.1 con Gradle Wrapper

- **Estado:** Aceptada
- **Fecha:** 2026-10-05
- **Etapa:** 1
- **Origen:** consigna (Spring Boot) y decisión del equipo (versión y Gradle)

## Contexto
La consigna exige Spring Boot. El equipo trabaja con Gradle, pero Gradle no estaba instalado en el entorno, y todos los integrantes tienen que poder compilar el proyecto de la misma forma.

## Decisión
- Usar **Spring Boot 4.1.1**, la última versión estable al crear el proyecto.
- Generar el proyecto con **Spring Initializr**, que incluye el **Gradle Wrapper** (`gradlew`, Gradle 9.7.1).
- Dependencias iniciales: Spring Web MVC, Spring Data JPA (starter), HSQLDB y los starters de test.

## Alternativas consideradas
- **Spring Boot 3.5.x:** es la rama anterior. Para un proyecto nuevo conviene la versión actual.
- **Maven:** igual de válido, pero el equipo usa Gradle.
- **Instalar Gradle en cada equipo:** genera diferencias de versión entre integrantes. El wrapper fija la versión.

## Consecuencias
- Basta con el JDK para compilar: el wrapper descarga la versión correcta de Gradle.
- El starter de Spring Data JPA incluye Spring Data, pero sus repositorios no se usan (ver [ADR-0004](0004-persistencia-con-entitymanager-sin-repository.md)).
- Las demás dependencias se agregan recién en el incremento que las usa.

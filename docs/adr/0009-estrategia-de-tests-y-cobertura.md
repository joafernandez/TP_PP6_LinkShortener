# ADR-0009 – Tests con JUnit 5, Spring Boot Test, MockMvc y cobertura con JaCoCo

- **Estado:** Aceptada
- **Fecha:** 2026-10-05
- **Etapa:** 1
- **Origen:** consigna (§3.4, cobertura de pruebas unitarias y de integración)

## Contexto
La consigna exige verificar la calidad con pruebas unitarias y de integración y medir su cobertura.

## Decisión
- **Tests unitarios** con JUnit 5 para la lógica sin infraestructura: generación de alias, cálculo de vencimiento con un `Clock` fijo, validaciones.
- **Tests de integración** con `@SpringBootTest` y **MockMvc** sobre HSQLDB en memoria, idealmente uno por criterio de aceptación de cada historia de usuario.
- **Cobertura con JaCoCo**, que genera un reporte HTML y XML después de cada `gradlew test`. El umbral mínimo se acuerda en el incremento 8.
- Los tests van en el mismo paquete que la clase que prueban, dentro de `src/test/java`.

## Alternativas consideradas
- **Solo tests de integración:** son más lentos y localizan peor los errores.
- **Testcontainers u otros motores de base para los tests:** innecesario, porque HSQLDB en memoria es el mismo motor que se usa en desarrollo.

## Consecuencias
- Cada criterio de aceptación queda verificado automáticamente, lo que da calidad funcional.
- El reporte de cobertura sirve de evidencia para el QA de cada etapa.
- JaCoCo es un plugin de Gradle y no agrega dependencias en tiempo de ejecución.

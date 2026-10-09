# 007 – Validación básica de configuración al arrancar

- **Fecha:** 2026-10-08
- **Herramienta:** Codex
- **Objetivo:** detectar errores básicos de configuración durante el arranque, con una implementación pequeña y mensajes claros.
- **Estado:** implementado y verificado; guardado en Git a pedido del equipo, sin push.

## Pedido del equipo

El equipo pidió explicar la mejora de validación de configuración y luego preguntó: «no es muy complejo? aporta robustez?». Se redujo la propuesta a tres comprobaciones: largo del alias entre 1 y 16, duración positiva y URL base HTTP/HTTPS con host válido. La aprobación fue «dale».

Tras recibir los resultados y las explicaciones sobre la configuración y el TTL, el equipo pidió «commitea», autorizando guardar este cambio con sus pruebas y documentación. El push sigue sin autorización.

La propuesta más amplia de comprobar todas las combinaciones de alias reservados se dejó para después. Este cambio tampoco agrega restricciones nuevas sobre el alfabeto, las rutas de la URL base, sus parámetros o su puerto. Se conserva el alcance simplificado y las dependencias existentes.

## Diseño e implementación

- Mantener la configuración centralizada en `AppProperties`, ya anotada con `@Validated`, con validación anidada mediante `@Valid`.
- Agregar `@Max(16)` al largo del alias y un mensaje claro al `@Min(1)` existente. El máximo coincide con la columna `link.alias`.
- Agregar `isTtlPositive()` con `@AssertTrue`: rechazar duración cero o negativa. El `@NotNull` existente informa si falta la duración.
- Agregar `isBaseUrlValid()` con `@AssertTrue`: analizar la URL con `URI` y exigir esquema HTTP/HTTPS y host válido. `@NotBlank` informa si falta la URL.
- Los mensajes identifican las propiedades `app.alias.length`, `app.link.ttl` y `app.base-url` en español.
- Los valores actuales de la aplicación se conservan.

## Pruebas y resultados

- Se creó `AppPropertiesTest` con `ApplicationContextRunner` y un contexto que habilita la configuración real mediante `@EnableConfigurationProperties`. Comprueba el binding y la validación de Spring sin abrir una base de datos.
- **16 casos nuevos:** valores actuales, largos límite 1 y 16, URL HTTP local con puerto y barra final, HTTPS, IP, esquema en mayúsculas; rechazo de largo 0 o 17, duración cero o negativa, esquema distinto, URL relativa, host ausente, sintaxis inválida y URL vacía.
- Los casos inválidos comprueban que el contexto falla con `BindValidationException` y el mensaje correspondiente.
- Comando desde `backend/`, con el JDK 25 portable existente: `gradlew test --offline --no-daemon`.
- Resultado: **BUILD SUCCESSFUL**, **105 tests, 0 fallos, 0 errores y 0 omitidos** (89 existentes y 16 nuevos).
- Cobertura total JaCoCo: **96,72 % de instrucciones**, **96,17 % de líneas** y **93,42 % de ramas**.
- Se actualizaron `docs/arquitectura.md`, el índice de prompts y `CLAUDE.md`. Código, pruebas y documentación se guardan juntos en Git tras el pedido explícito del equipo. No se hizo push.

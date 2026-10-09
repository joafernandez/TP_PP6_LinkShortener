# 006 – Corrección: reintentos únicamente por colisión de alias

- **Fecha:** 2026-10-08
- **Herramienta:** Codex
- **Objetivo:** distinguir una colisión del alias de otros errores de persistencia, conservando las transacciones por intento y la causa del error.
- **Estado:** implementado y verificado; guardado en Git a pedido del equipo, sin push.

## Pedido del equipo

Tras una revisión de mejoras, el equipo pidió explicar cómo resolver el manejo demasiado general de errores y la validación de configuración. Luego autorizó únicamente el primer cambio: «implementa el 1».

Durante la implementación se respetó la indicación previa de no hacer commit ni push. Tras recibir los resultados, el equipo pidió «commitea», autorizando guardar esta corrección con sus pruebas y documentación. El push sigue sin autorización. La mejora de configuración (punto 2), los encabezados de caché de redirección y el incremento 6 no forman parte de esta corrección.

## Diseño

- Conservar el `catch` de errores de persistencia en `LinkService.create`, pero reintentar solo una colisión confirmada.
- Recorrer las causas hasta encontrar la `ConstraintViolationException` de Hibernate. Comprobar el SQLState `23505` y el nombre exacto de la restricción existente: `uk_link_alias`, opcionalmente calificado con el esquema `PUBLIC`, sin distinguir mayúsculas.
- Si no se identifica esa restricción, propagar la misma excepción. No clasificar errores mediante palabras del mensaje ni reintentar otras restricciones UNIQUE.
- Mantener el máximo de intentos y la respuesta de alias no disponible cuando se agotan colisiones confirmadas.
- Mantener la migración existente y las dependencias del proyecto.

## Pruebas

- Unitarias del servicio: colisión reconocida, wrappers de Spring y JPA, causas anidadas, límite de intentos, otra restricción, restricción desconocida, nombre similar, SQLState distinto y errores generales. Verificar la identidad del error propagado y los rollback/commit.
- Integración con HSQLDB en memoria: ocultar únicamente la primera lectura de un alias existente para provocar una inserción duplicada real. Verificar el SQLState y el nombre reportados por Hibernate, el rollback y la creación posterior con otro alias, conservando el destino original.
- Esta prueba controla la lectura para reproducir la colisión de forma determinista; no es una prueba de dos peticiones concurrentes.
- Integración de un error distinto: el generador de prueba devuelve un alias de 17 caracteres; el error de la base debe propagarse tras un único intento y no dejar registros.

## Resultado

- Primera ejecución de `gradlew test --tests app.link.LinkServiceTest --tests app.link.LinkServiceIT --offline --no-daemon`: 25 casos, 1 fallo en la captura de la excepción del test de integración. El servicio sí reintentó correctamente; al usar IDENTITY, Hibernate insertó durante `persist`, antes del `flush` donde el test intentaba observar la excepción. Se corrigió la observación en el test, manteniendo las aserciones sobre el error real y los registros.
- Ejecución final de `gradlew test --offline --no-daemon`, con el JDK 25 portable existente: **BUILD SUCCESSFUL**, **89 tests, 0 fallos, 0 errores y 0 omitidos**. Son los 76 existentes más 11 casos unitarios y 2 de integración.
- La colisión real informó SQLState **23505** y restricción **UK_LINK_ALIAS**. Se verificó que el registro original conserva su destino y que el siguiente intento guarda el nuevo enlace con otro alias.
- El alias demasiado largo produjo SQLState **22001**, se propagó tras un solo intento y la transacción no dejó registros.
- Cobertura total JaCoCo: **96,48 % de instrucciones**, **96,00 % de líneas** y **96,55 % de ramas**.
- Solo se usó HSQLDB en memoria; no se modificó la base persistente ni la configuración de producción.
- Se actualizaron el flujo de creación en `docs/arquitectura.md`, el índice de prompts y el contexto de continuación en `CLAUDE.md`. Código, pruebas y documentación se guardan juntos en Git tras el pedido explícito del equipo. No se hizo push.

# 010 – Capa Repository para los enlaces

- **Fecha:** 2026-10-11.
- **Herramienta:** Codex.
- **Objetivo:** separar la persistencia del negocio manteniendo el comportamiento del MVP.

## Pedido del equipo

Tras traducir los nombres del negocio y guardar ese cambio en el commit `9270501`, el usuario pidió explicar cómo incorporar Repository. Aclaró que agregar la capa era una decisión del equipo y que inicialmente se había omitido para priorizar el MVP.

Se propuso una clase concreta `EnlaceRepository` con `EntityManager`, conservando el service como coordinador. Se acordó trasladar las consultas y operaciones de persistencia, clasificar las colisiones con una excepción propia y conservar las transacciones por intento. El usuario aceptó la propuesta y autorizó implementarla. Después de la verificación pidió «commitea», autorizando guardar juntos el código, las pruebas y la documentación. No pidió push.

## Diseño y archivos afectados

- Crear `EnlaceRepository` y `ColisionAliasException` dentro de `app.enlace`.
- Adaptar `EnlaceService` para depender del Repository y reintentar únicamente la excepción de colisión propia.
- Adaptar `EnlaceServiceTest` para simular el Repository, sin depender de JPA, Hibernate o SQL.
- Agregar `EnlaceRepositoryTest` para clasificación de errores en `persist` y `flush`, conservación de causas y propagación de errores ajenos al alias.
- Adaptar `EnlaceServiceIT` para construir el Repository en la simulación de colisión real. Comprobar los otros errores a través de la traducción estándar de `@Repository`, preservando la causa SQL.
- Agregar `EnlaceRepositoryIT` con el bean real para comprobar colisión, rollback, inserción posterior y actualización condicional frente a una lectura anterior a la renovación.
- Crear ADR-0024, marcar ADR-0004 como reemplazado y actualizar arquitectura, README, índices y contexto de continuación.
- Mantener la base, las migraciones, el contrato HTTP, configuración, dependencias y frontend.

## Validación

- `gradlew test bootJar --offline --no-daemon`: **125 tests**, sin fallos, errores ni omitidos; build exitoso.
- JaCoCo del backend: **97,20 % de instrucciones**, **96,92 % de líneas** y **93,42 % de ramas**. No representa cobertura del JavaScript del frontend.
- Las pruebas verifican clasificación en `persist` y `flush`, conservación de la causa, propagación de otros errores, rollback antes de reintentar y agotamiento de intentos. La integración comprueba una colisión real con el bean Repository, recuperación en otra transacción y que una lectura anterior no sobrescriba un alias renovado.
- En la primera ejecución se corrigieron tres verificaciones de tests: dos lecturas directas del Repository se movieron dentro de `TransactionTemplate`, y la conservación de `SQLException` se comprobó en la cadena de causas (la causa más profunda es interna de HSQLDB). Se mantuvo el diseño de transacciones coordinadas por el service.
- Se inspeccionó el JAR: incluye `EnlaceRepository`, `ColisionAliasException` y la página web empaquetada.
- `git diff --check` pasó. La revisión del diff confirmó que no se modificaron dependencias, esquema, migraciones, configuración, contrato OpenAPI ni fuentes del frontend.
- En este entorno se usó el JDK 25 portable existente. El comando requirió ejecución fuera del entorno restringido porque este bloqueaba la lectura de `conf/security/java.security`; no se descargaron dependencias.

## Resultado

El acceso a datos quedó separado en un único Repository concreto. El service mantiene la coordinación del negocio y de las transacciones, sin consultar JPA ni clasificar errores SQL. Se reducen sus motivos de cambio y las pruebas unitarias dependen de operaciones del Repository en lugar de detalles de persistencia. No se separaron clases por caso de uso.

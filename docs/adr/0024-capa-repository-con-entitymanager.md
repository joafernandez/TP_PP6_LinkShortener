# ADR-0024 – Capa Repository propia con EntityManager

- **Estado:** Aceptada.
- **Fecha:** 2026-10-11.
- **Etapa:** 1.
- **Origen:** decisión del equipo.
- **Reemplaza:** [ADR-0004](0004-persistencia-con-entitymanager-sin-repository.md).

## Contexto

El MVP concentró negocio y acceso a datos en el service para reducir la estructura inicial. El equipo aclaró que esa simplificación respondió a priorizar un producto funcional, y decidió incorporar Repository al revisar el diseño. La ausencia de esta capa deja de ser un lineamiento vigente.

`EnlaceService` conocía JPQL, `persist`, `flush`, `refresh`, el SQLState de HSQLDB y el nombre de la restricción UNIQUE del alias. Su lógica podía cambiar por reglas de negocio o por detalles de persistencia. Las pruebas unitarias del service también necesitaban simular `EntityManager`, consultas y excepciones SQL.

## Decisión

- Crear una clase concreta `EnlaceRepository` dentro de `app.enlace`, con `@Repository` e inyección por constructor de `EntityManager`.
- Concentrar búsqueda por alias, inserción y reasignación condicional. Mantener JPQL parametrizado y la actualización atómica con `venceEn <= ahora`.
- El Repository recibe las fechas calculadas por el service, sin depender del reloj, propiedades, generador de alias ni controllers.
- Mantener `TransactionTemplate` en el service para delimitar cada intento. La colisión sale del intento, provoca rollback y se maneja antes de comenzar el siguiente. `resolver` conserva su transacción de lectura.
- Conservar `persist` y `flush` dentro del mismo manejo de errores: con IDENTITY la inserción puede fallar ya en `persist`.
- Traducir únicamente la colisión confirmada de `uk_link_alias` (o `PUBLIC.uk_link_alias`, sin distinguir mayúsculas) con SQLState `23505` a `ColisionAliasException`, conservando la causa original. Otros errores no habilitan reintentos y conservan sus causas; Spring puede traducirlos a su jerarquía `DataAccessException` por tratarse de un `@Repository`.
- La reasignación refresca la entidad cuando se actualiza una fila, para devolver el estado realmente persistido. Si el alias ya está vigente, devuelve `false` y permite probar otro candidato.
- Mantener `EnlaceService` como coordinador de operaciones relacionadas: crear, resolver y construir la URL corta. No separar una clase por método.
- Usar una clase concreta sin interfaz adicional, repositorios genéricos ni nuevas dependencias. El QR no requiere Repository porque se genera en memoria.

## Alternativas consideradas

- **Mantener EntityManager en el service:** conserva menos archivos, pero mantiene los motivos de cambio y las pruebas acopladas a detalles de JPA.
- **Spring Data JPA:** simplifica CRUD estándar; en este proyecto las operaciones y el manejo de colisiones ya existen, por lo que trasladarlas a un Repository propio requiere menos adaptación y mantiene visible el control de persistencia.
- **Interfaz más implementación:** posible si se necesitan implementaciones alternativas. Una clase concreta es suficiente para el alcance actual y puede simularse con Mockito.

## Consecuencias

- El service deja de conocer consultas, operaciones JPA, excepciones Hibernate y códigos SQL. Sigue coordinando transacciones de Spring y reglas del negocio.
- Se reducen sus motivos de cambio y se simplifican las pruebas unitarias mediante un mock del Repository.
- El Repository queda acoplado a JPA/Hibernate y concentra las particularidades de clasificación de errores de HSQLDB.
- Se agregan dos clases de producción y pruebas específicas. La separación se comprueba con colisiones reales y una actualización basada en una lectura anterior a la renovación del alias.
- No cambia el contrato HTTP, el esquema de la base, la configuración ni el frontend.

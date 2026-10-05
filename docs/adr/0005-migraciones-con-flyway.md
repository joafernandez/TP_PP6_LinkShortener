# ADR-0005 – Esquema de base de datos con migraciones Flyway

- **Estado:** Aceptada
- **Fecha:** 2026-10-05
- **Etapa:** 1
- **Origen:** decisión del equipo

## Contexto
La consigna anticipa que el Cliente cambiará los requerimientos en cada etapa, así que el modelo de datos va a evolucionar. Hay que poder reproducir esos cambios de esquema y saber cuándo y por qué se hizo cada uno.

## Decisión
- El esquema se define con **migraciones versionadas de Flyway** (`backend/src/main/resources/db/migration/V{n}__descripcion.sql`).
- Hibernate no crea ni modifica tablas. Primero se usa `ddl-auto=none` y, al incorporar Flyway, `validate`, para que Hibernate controle que las entidades coincidan con el esquema.
- Flyway se agrega en el incremento 3, junto con la primera tabla.

## Alternativas consideradas
- **`ddl-auto=update`:** más simple, pero los cambios no quedan registrados, no se pueden repetir y pueden dejar el esquema inconsistente.
- **Script `schema.sql` único:** no registra la evolución entre etapas.

## Consecuencias
- Cada cambio de esquema queda en un archivo versionado en git, alineado con las etapas del TP.
- Se agrega una dependencia (Flyway y su módulo para HSQLDB).
- Una migración ya aplicada no se edita: los cambios se hacen siempre con una migración nueva.

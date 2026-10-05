# ADR-0003 – HSQLDB en modo archivo, y en memoria para los tests

- **Estado:** Aceptada
- **Fecha:** 2026-10-05
- **Etapa:** 1
- **Origen:** consigna (base relacional con JPA/Hibernate) y decisión del equipo (motor)

## Contexto
La consigna exige persistir con JPA/Hibernate sobre *"un motor de base de datos relacional"*, sin indicar cuál. Para la Etapa 1 se busca un motor sin instalación ni servidor aparte.

## Decisión
- **HSQLDB embebido en modo archivo** para desarrollo y ejecución. Los datos quedan en `database/`, con una ruta configurable mediante la variable `APP_DB_PATH`.
- **HSQLDB en memoria** para los tests, así no dependen de la base real ni la modifican.
- Los archivos de la base no se versionan en git.

## Alternativas consideradas
- **HSQLDB solo en memoria:** los datos se pierden al reiniciar la aplicación.
- **PostgreSQL o MySQL:** requieren instalar y administrar un servidor. Es innecesario para el alcance actual.
- **H2:** equivalente a HSQLDB, pero el equipo ya trabaja con HSQLDB.

## Consecuencias
- No hay que instalar nada: la base se crea al arrancar la aplicación.
- Al usar JPA y JPQL estándar, migrar a otro motor relacional requiere cambiar la configuración y las migraciones, no el código de negocio.
- HSQLDB embebido no admite varios procesos accediendo al mismo archivo. Es suficiente para la Etapa 1.

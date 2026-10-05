# ADR-0004 – Persistencia con EntityManager y JPQL, sin capa Repository

- **Estado:** Aceptada
- **Fecha:** 2026-10-05
- **Etapa:** 1
- **Origen:** decisión del equipo (lineamientos técnicos de la cátedra)

## Contexto
La consigna exige JPA/Hibernate. Los lineamientos con los que trabaja el equipo indican usar `EntityManager` y JPQL/HQL directamente desde la capa Service, sin una capa Repository.

## Decisión
- Los services acceden a la persistencia mediante un `EntityManager` inyectado.
- Las consultas se escriben en JPQL con **parámetros nombrados**, nunca concatenando strings.
- Cada consulta va en un **método privado con nombre descriptivo** dentro del service, para que la lógica de negocio no se mezcle con el JPQL.
- Las transacciones se declaran con `@Transactional` en el service.

## Alternativas consideradas
- **Repositorios de Spring Data JPA:** menos código, pero se aparta de los lineamientos.
- **Capa DAO propia sobre `EntityManager`:** separa mejor la persistencia del negocio, pero agrega una capa que la Etapa 1 no necesita.

## Consecuencias
- Hay una capa menos y la persistencia queda explícita.
- El service queda acoplado a JPA. Testear su lógica de forma unitaria requiere simular el `EntityManager` o recurrir a tests de integración.
- **Criterio para revisar esta decisión:** si un service maneja más de una entidad, o si sus tests unitarios se vuelven difíciles de mantener, se propondrá extraer la persistencia a un DAO o Repository, con un nuevo ADR y aprobación previa.

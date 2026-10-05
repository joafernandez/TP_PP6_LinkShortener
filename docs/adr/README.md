# Registro de decisiones de arquitectura (ADR)

Cada decisión de diseño o arquitectura relevante se registra en un archivo propio, con su contexto, las alternativas consideradas y sus consecuencias. Sirve para justificar el diseño ante el Cliente y para evaluar el impacto cuando cambien los requerimientos en las etapas siguientes.

## Convenciones

- Nombre del archivo: `NNNN-titulo-corto.md`, con numeración correlativa.
- Una decisión por archivo.
- Una decisión no se borra. Si cambia, se crea un ADR nuevo y el anterior pasa a estado **Reemplazada por ADR-NNNN**.
- Estados posibles: **Propuesta**, **Aceptada**, **Reemplazada**, **Descartada**.

## Plantilla

```markdown
# ADR-NNNN – Título

- **Estado:** Propuesta | Aceptada | Reemplazada por ADR-NNNN
- **Fecha:** AAAA-MM-DD
- **Etapa:** N
- **Origen:** consigna | decisión del equipo | respuesta del Cliente (minuta NN)

## Contexto
Qué problema o necesidad motiva la decisión.

## Decisión
Qué se decidió.

## Alternativas consideradas
Qué otras opciones había y por qué no se eligieron.

## Consecuencias
Qué se gana, qué se pierde y qué queda condicionado a futuro.
```

## Índice

| ADR | Decisión | Estado | Se aplica en |
|---|---|---|---|
| [0001](0001-version-de-java.md) | Java 25 como versión del JDK | Aceptada | Incremento 2 ✅ |
| [0002](0002-spring-boot-y-gradle-wrapper.md) | Spring Boot 4.1.1 con Gradle Wrapper | Aceptada | Incremento 2 ✅ |
| [0003](0003-base-de-datos-hsqldb.md) | HSQLDB en modo archivo, y en memoria para los tests | Aceptada | Incremento 2 ✅ |
| [0004](0004-persistencia-con-entitymanager-sin-repository.md) | Persistencia con EntityManager y JPQL, sin capa Repository | Aceptada | Incremento 3 |
| [0005](0005-migraciones-con-flyway.md) | Esquema de base de datos con migraciones Flyway | Aceptada | Incremento 3 |
| [0006](0006-paquetes-por-funcionalidad.md) | Organización del código por funcionalidad bajo el paquete `app` | Aceptada | Incremento 2 ✅ y siguientes |
| [0007](0007-diseno-de-la-api-rest.md) | API REST versionada `/api/v1`, DTOs y errores con ProblemDetail | Aceptada | Incremento 3 |
| [0008](0008-contrato-openapi-contract-first.md) | Contrato OpenAPI escrito antes del código, más springdoc | Aceptada | Incrementos 1 y 3 |
| [0009](0009-estrategia-de-tests-y-cobertura.md) | Tests con JUnit 5, Spring Boot Test, MockMvc y cobertura con JaCoCo | Aceptada | Incremento 2 ✅ y siguientes |
| [0010](0010-puntos-de-extension.md) | Puntos de extensión: `AliasGenerator`, propiedades configurables y `Clock` | Aceptada | Incrementos 3 y 4 |
| [0011](0011-ruta-del-alias-y-palabras-reservadas.md) | Ruta `/{alias}` alfanumérica y lista de palabras reservadas | Aceptada | Incremento 4 |
| [0012](0012-redireccion-con-302.md) | Redirección con HTTP 302 | Aceptada | Incremento 4 |
| [0013](0013-cliente-web-sin-framework.md) | Página web en HTML, CSS y JS sin framework, servida por Spring Boot | Aceptada | Incremento 6 |
| [0014](0014-extension-mv3-y-permisos-de-host.md) | Extensión MV3 única para Chrome y Firefox, con permisos de host en lugar de CORS | Aceptada | Incremento 7 |
| [0015](0015-control-de-versiones.md) | Control de versiones con git y un tag por etapa | Aceptada | Incremento 2 ✅ |

### Supuestos del equipo, pendientes de confirmación del Cliente

Ante las ambigüedades D1 a D8 de la [minuta 01](../etapa-1/minutas/minuta-01-elucidacion.md), el equipo adoptó las siguientes decisiones para no frenar el desarrollo. Si el Cliente responde distinto, se crea un ADR nuevo que reemplaza al correspondiente.

| ADR | Decisión | Duda | Se aplica en |
|---|---|---|---|
| [0016](0016-formato-del-alias.md) | Alias alfanumérico aleatorio de 5 caracteres, sin caracteres ambiguos | D1 | Incremento 3 |
| [0017](0017-misma-url-alias-nuevo.md) | Acortar de nuevo una URL genera un alias nuevo | D2 | Incremento 3 |
| [0018](0018-enlace-vencido-o-inexistente.md) | Enlace vencido o inexistente: página HTML con código 404 | D3 | Incremento 4 |
| [0019](0019-rechazo-de-urls-del-propio-servicio.md) | Se rechaza acortar URLs del propio servicio | D4 | Incremento 3 |
| [0020](0020-criterio-de-url-valida.md) | URL válida: absoluta, `http`/`https`, con host, hasta 2048 caracteres | D5 | Incremento 3 |
| [0021](0021-sin-historial-de-enlaces.md) | Sin historial: el alias vencido se reasigna reutilizando su registro | D6 | Incrementos 3 y 4 |
| [0022](0022-dominio-configurable.md) | Dominio o IP configurable en el servidor y en la extensión | D7 | Incrementos 3 y 7 |
| [0023](0023-qr-generado-en-el-servidor.md) | Código QR generado en el servidor (ZXing) | D8 | Incremento 5 |

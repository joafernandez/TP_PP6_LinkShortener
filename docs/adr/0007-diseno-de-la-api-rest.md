# ADR-0007 – API REST versionada `/api/v1`, DTOs y errores con ProblemDetail

- **Estado:** Aceptada
- **Fecha:** 2026-10-05
- **Etapa:** 1
- **Origen:** consigna (API REST y buenas prácticas REST) y decisión del equipo

## Contexto
La consigna exige una API REST consumida por la página web y por la extensión, y evalúa el cumplimiento de buenas prácticas REST. La extensión queda instalada en los navegadores, por lo que un cambio incompatible en la API puede romperla.

## Decisión
- Todos los endpoints de la API van bajo el prefijo **`/api/v1`**.
- Los recursos se nombran en plural (`/api/v1/links`).
- Códigos de estado HTTP estándar, por ejemplo `201 Created` con encabezado `Location` al crear y `400` ante una entrada inválida.
- **DTOs** para entrada y salida. Las entidades JPA nunca se exponen en la API.
- Errores con **`ProblemDetail`** (RFC 9457), centralizados en un `@RestControllerAdvice`.
- La entrada se valida con **Bean Validation** (`spring-boot-starter-validation`), que se agrega en el incremento 3.

## Alternativas consideradas
- **Sin versionado:** un cambio incompatible en la Etapa 2 rompería las extensiones ya instaladas.
- **Exponer las entidades directamente:** acopla el contrato al modelo de datos, y cualquier cambio en la tabla rompería a los clientes.
- **Formato de error propio:** ProblemDetail es estándar y Spring lo soporta sin configuración extra.

## Consecuencias
- Un cambio incompatible puede publicarse como `/api/v2` y convivir con `v1`.
- El modelo de datos y el contrato pueden evolucionar por separado.
- Hay que mapear entre entidades y DTOs.
- La redirección `/{alias}` queda fuera de `/api/v1`, porque es parte de la URL corta (ver [ADR-0011](0011-ruta-del-alias-y-palabras-reservadas.md)).

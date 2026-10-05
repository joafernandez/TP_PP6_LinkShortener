# ADR-0008 – Contrato OpenAPI escrito antes del código, más springdoc

- **Estado:** Aceptada
- **Fecha:** 2026-10-05
- **Etapa:** 1
- **Origen:** consigna (§3, fase de Diseño y Especificación)

## Contexto
La consigna pide definir los *"contratos de la API REST (OpenAPI/Swagger)"* en la fase de diseño, **antes de la generación de código**.

## Decisión
- **Contract-first:** el contrato se escribe a mano en `docs/api/openapi.yaml` durante el diseño (incremento 1) y se revisa antes de implementar.
- **springdoc-openapi** se agrega en el incremento 3 para publicar Swagger UI y la especificación generada a partir del código.
- Durante el QA se compara la especificación generada con el contrato escrito, y cualquier diferencia se corrige.

## Alternativas consideradas
- **Solo springdoc (code-first):** el contrato saldría del código, lo que va en contra de lo que pide la consigna.
- **Generar el código a partir del contrato** (openapi-generator): agrega complejidad de build innecesaria para el tamaño del proyecto.

## Consecuencias
- El contrato es el acuerdo entre el backend y sus clientes, y se puede revisar antes de escribir código.
- Hay que mantener sincronizados el contrato y la implementación. El chequeo de QA lo controla.
- Swagger UI queda disponible para probar la API manualmente.

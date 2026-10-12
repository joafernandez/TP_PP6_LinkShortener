# 013 – Incremento 8: QA de la Etapa 1

- **Fecha:** 2026-10-11.
- **Herramienta:** Claude (claude.ai).
- **Objetivo:** cerrar la etapa con el control de calidad: umbral de cobertura, revisión REST y JPA, contraste con OpenAPI e informe de QA.
- **Estado:** implementado y revisado de forma estática; **pendientes las corridas del equipo** (tests con el nuevo umbral y prueba manual de la extensión).

## Pedido del equipo

La integrante responsable de los incrementos 7 y 8 pidió ejecutar el incremento 8 tras aprobar la lista de archivos a modificar y los valores del umbral de cobertura propuestos (90 % de instrucciones y de líneas, 85 % de ramas). Restricciones indicadas: no usar Node en el proyecto y explicar en pasos pequeños con equivalencias con PHP. El análisis partió del repositorio tal como lo dejó su compañera, con el código del negocio en español y la capa Repository.

## Diseño antes del código

- El [ADR-0009](../../adr/0009-estrategia-de-tests-y-cobertura.md) dejó el umbral de cobertura para este incremento. Se fijó en el [ADR-0025](../../adr/0025-umbral-de-cobertura.md) y se verifica con `jacocoTestCoverageVerification`, que se ejecuta después del reporte para que este se genere siempre.
- El contraste con OpenAPI se hizo elemento por elemento (rutas, códigos, encabezados, esquemas y `operationId`) contra los controllers, el manejador de errores y los tests.
- La revisión REST usó el [ADR-0007](../../adr/0007-diseno-de-la-api-rest.md) como criterio; la de JPA comparó la entidad con la migración de Flyway y revisó el manejo de transacciones y colisiones.
- La trazabilidad se armó leyendo los nombres de los tests, que no incluyen el identificador del criterio.

## Resultados

- **Informe:** [docs/etapa-1/qa.md](../qa.md), con la matriz criterio → verificación, el contraste con OpenAPI, las revisiones REST y JPA, los casos de prueba manual y la lista de hallazgos.
- **Hallazgo corregido (H-01):** el `operationId` de crear un enlace era `create` y el contrato dice `createLink`. El código ya lo generaba así antes del cambio de nombres. Se ajustó `EnlaceController` y la aserción de `QrControllerIT` que fijaba el valor anterior.
- **Umbral:** se agregó la regla de JaCoCo en `backend/build.gradle`; `gradlew test` falla si la cobertura baja de 90 %, 90 % y 85 %.
- **Abiertos:** un test HTTP del `503` (H-02) y la corrección de una oración de `docs/arquitectura.md` (H-06).
- **No se ejecutó nada** en esta sesión: ni los tests, ni el backend, ni Gradle. El cambio de `build.gradle` y el de los dos archivos Java deben validarse con `.\tests.ps1` antes de darlos por buenos. Las cifras de cobertura del informe son las que registró el equipo en los prompts 010 y 011, y la corrida actual figura como pendiente.

# ADR-0025 – Umbral mínimo de cobertura con JaCoCo

- **Estado:** Aceptada.
- **Fecha:** 2026-10-11.
- **Etapa:** 1.
- **Origen:** decisión del equipo (el [ADR-0009](0009-estrategia-de-tests-y-cobertura.md) dejó el umbral para el incremento 8).

## Contexto

JaCoCo genera el reporte de cobertura después de cada `gradlew test`, pero nada impide que la cobertura baje sin que nadie lo note. El ADR-0009 pospuso la definición del umbral mínimo. Las cifras informadas por el equipo al cerrar el [prompt 010](../etapa-1/prompts/010-capa-repository.md) son 97,20 % de instrucciones, 96,92 % de líneas y 93,42 % de ramas.

## Decisión

- La cobertura del backend, medida sobre todo el paquete `app`, debe ser como mínimo de **90 % de instrucciones**, **90 % de líneas** y **85 % de ramas**.
- La regla se verifica con `jacocoTestCoverageVerification` en `backend/build.gradle`. Se ejecuta después del reporte, que se genera siempre, y hace fallar `gradlew test` si algún valor queda por debajo.
- No se excluyen clases de la medición.

## Alternativas consideradas

- **100 %:** obliga a probar código sin valor, como el punto de entrada o los constructores triviales, y vuelve frágil la build.
- **Umbral por clase o paquete:** más preciso, pero con unas veinte clases de producción agrega mantenimiento sin un beneficio claro en esta etapa.
- **Seguir sin umbral:** deja el reporte como dato informativo; no protege la calidad ante cambios futuros.

## Consecuencias

- Hay un margen de unos 7 puntos entre el umbral y las cifras actuales: absorbe cambios chicos sin tests nuevos, pero una funcionalidad grande sin pruebas hace fallar la build.
- Las etapas siguientes heredan la regla. Si el umbral se vuelve incómodo, se ajusta con un ADR nuevo y no relajándolo en silencio.
- Solo se mide el código Java. La extensión (JavaScript) no tiene pruebas automáticas, porque el proyecto no usa Node; se verifica con la prueba manual de [extension/README.md](../../extension/README.md).
- Cuando `tests.ps1` falla por el umbral, el script abre solo el reporte de tests; el de cobertura queda en `backend/build/reports/jacoco/test/html/index.html`.

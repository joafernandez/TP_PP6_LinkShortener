# 004 – Incremento 4: redirección

- **Fecha:** 2026-10-06
- **Herramienta:** Claude Code (VS Code)
- **Objetivo:** que la URL corta redirija a la URL original, y que muestre una página 404 si está vencida o no existe (HU-04, ADR-0012, ADR-0018).

## Secuencia de prompts (resumida)

| # | Prompt del equipo (resumen) | Resultado |
|---|---|---|
| 1 | "¿Cómo pruebo lo que tengo hasta el momento?" | Instrucciones para correr los tests y probar con Swagger UI en el puerto 8081 |
| 2 | Confirmación del estado del proyecto (captura de la tabla de avance) | La IA confirmó el estado: falta la redirección |
| 3 | "¿Cuál es la propuesta del incremento 4?" | Lista de archivos, dos decisiones técnicas con recomendación y tests por criterio de aceptación |
| 4 | "Sí" (aprobación) | Implementación, tests y verificación manual |

## Decisiones técnicas tomadas en la propuesta

- **Página 404 en `resources/pages/`**, devuelta por el controller. Así no hace falta un motor de plantillas, y la página no se puede abrir como archivo estático suelto.
- **`@ExceptionHandler` local en `RedirectController`**, que tiene prioridad sobre el manejador global de la API. La API sigue respondiendo JSON y la redirección responde HTML.

## Resultado

- `LinkNotFoundException`, `LinkService.resolve`, `RedirectController` y `link-not-found.html`.
- Se extrajo `TestClockConfig` a `app.support` para reutilizar el reloj de prueba.
- 66 tests (12 nuevos), con 95% de cobertura. Todos pasaron en la primera ejecución.
- Verificación manual: `POST` → `shortUrl`. Al abrirla: 302 a Google. Con un alias inventado: 404 HTML "Enlace no disponible". Swagger sigue respondiendo 200.

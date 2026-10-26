# 001 – Análisis de impacto de la Etapa 2

- **Fecha:** 2026-10-25
- **Herramienta:** Claude Code (VS Code)
- **Objetivo:** analizar el impacto de los requerimientos de la Etapa 2 sobre el sistema de la Etapa 1, sin modificar código, y proponer un plan incremental.
- **Estado:** análisis aprobado; documentación de la primera parte escrita (incremento A.1).

## Prompt (resumido)

El equipo pidió revisar el estado completo del proyecto (`CLAUDE.md`, documentación, ADRs, OpenAPI, base, backend, frontend, extensión y tests) y analizar los nuevos requerimientos sin implementarlos:

- **Camino 1:** usuarios no logueados con códigos de 1 carácter siempre que sea posible, asignados de forma incremental (`/1`, `/2`), reutilizando los expirados y pasando a 2 caracteres al agotarse. Vigencia de 60 minutos.
- **Camino 2:** login con Google y con QR escaneado desde el celular (con autorización explícita), y un espacio de nombres único por usuario (`notes.io/melina/1`), en minúsculas, cambiable, con códigos automáticos o personalizados (`final`, `tp_java`) únicos dentro del espacio.
- **Extensión:** si hay sesión, crea los enlaces en el espacio del usuario; si no, sigue con el camino anónimo.

Se pidió analizar el impacto por área, detectar conflictos con los ADR, comparar alternativas para Google Login y login con QR, no agregar tecnologías sin justificar, proponer una división en **no más de 3 partes** e indicar las decisiones pendientes.

## Resultado

- **Se conserva:** validación de URL, vigencia y `Clock`, reasignación atómica de vencidos con reintentos por transacción, servicio de QR, manejo de errores, página 404, redirección 302 y `POST /api/v1/links` sin cambios de contrato.
- **Cambia:** la generación de códigos (ADR-0016 reemplazado), el contrato de `GeneradorAlias` (ADR-0010), la restricción `UNIQUE` del alias (ADR-0021, en el Camino 2), las rutas con dos segmentos y los nombres reservados (ADR-0011).
- **Modelo propuesto para el Camino 2:** entidades `Usuario` y `Espacio`, con el espacio anónimo como una fila más, y `UNIQUE(espacio_id, alias)`. Al cambiar de espacio se renombra la fila y se vencen los enlaces vigentes.
- **Autenticación recomendada:** Spring Security con `oauth2Login` y sesión por cookie para Google; un flujo propio de aprobación entre dispositivos para el QR, con polling. Se descartaron Google Identity Services en el navegador, proveedores externos, Device Authorization Grant y passkeys.
- **Riesgo principal:** Google exige HTTPS o `localhost`, lo que condiciona la demostración del login con QR desde el celular.
- **Plan:**
  - A. Códigos secuenciales para anónimos.
  - B. Login con Google, espacio de nombres y enlaces del usuario en la web.
  - C. Login con QR, extensión autenticada y QA.

## Decisiones a partir del resultado

- El equipo aprobó empezar por la parte A y adoptar provisoriamente dos supuestos: el **menor código libre** (E1) y el alfabeto `123456789abcdefghijkmnpqrstuvwxyz` (E2).
- Se registraron en la [minuta 02](../minutas/minuta-02-elucidacion.md), junto con las preguntas E3 a E10 para el Cliente, y en los ADR [0026](../../adr/0026-codigos-secuenciales.md) y [0027](../../adr/0027-generador-de-codigos-con-codigos-vigentes.md).
- Quedan pendientes de aprobación, antes de la parte B, las dependencias de Spring Security y el modelo de datos del Camino 2.

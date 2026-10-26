# Minuta 02 – Elucidación de requerimientos (Etapa 2)

| | |
|---|---|
| **Proyecto** | Servicio de Acortamiento y Gestión de Enlaces (Link Shortener) |
| **Cátedra** | Paradigmas de Programación VI |
| **Etapa** | 2 |
| **Fecha** | ____ / ____ / ______ |
| **Cliente** | ______________________ (docente) |
| **Equipo** | ______________________ |

## Objetivo de la reunión

Resolver con el Cliente los puntos de la Etapa 2 que los nuevos requerimientos no definen: códigos incrementales para usuarios no logueados (Camino 1), autenticación con Google y con QR, y espacio de nombres de los usuarios registrados (Camino 2).

Para cada punto se indica qué dice el requerimiento, las opciones que el equipo identificó y la propuesta del equipo.

> **Estado (2026-10-25):** para no frenar el desarrollo, el equipo adoptó provisoriamente las propuestas de E1 y E2, que afectan a la primera parte de la entrega (Camino 1). Están registradas en los ADR [0026](../../adr/0026-codigos-secuenciales.md) y [0027](../../adr/0027-generador-de-codigos-con-codigos-vigentes.md). Los puntos E3 a E10 se resuelven antes de implementar el Camino 2. Si alguna respuesta difiere, se registra en un ADR nuevo y se ajusta la implementación.

---

## Camino 1 – Usuarios no logueados

### E1 – Qué significa "incremental"

**Requerimiento:** el código debe tener 1 carácter siempre que sea posible, asignarse de forma incremental (`/1`, `/2`, `/3`), y los códigos de enlaces expirados quedan libres para reutilizarse.

**Opciones:**
- a) **Menor código libre:** cada pedido recibe el código libre más corto y, entre los del mismo largo, el primero en el orden del alfabeto. Si vence `/2`, el próximo pedido recibe `/2`.
- b) **Contador que avanza:** se asigna el siguiente del último entregado, y los vencidos se reutilizan recién al dar la vuelta.

**Propuesta del equipo:** a). Es la única que cumple a la vez "1 carácter siempre que sea posible" y "los códigos expirados quedan disponibles". Con b) se pasaría a 2 caracteres aunque hubiera códigos de 1 carácter libres.

**Preguntas al Cliente:**
- ¿Es correcta esta interpretación?

**Respuesta del Cliente:**

### E2 – Caracteres y orden de los códigos

**Requerimiento:** el ejemplo usa `/1`, `/2`, `/3`. No indica qué sigue después del último carácter.

**Opciones:**
- a) Solo dígitos (`1` a `9`, después `10`, `11`…): pocos códigos de 1 carácter.
- b) Dígitos y minúsculas, sin caracteres ambiguos.
- c) Dígitos, minúsculas y mayúsculas: más códigos de 1 carácter, pero más difícil de dictar.

**Propuesta del equipo:** b), en este orden: `123456789abcdefghijkmnpqrstuvwxyz` (33 símbolos; se excluyen `0`, `o` y `l`, que se confunden). Después de `/z` sigue `/11`, `/12`… Se usan minúsculas para ser coherentes con el espacio de nombres del Camino 2. El alfabeto queda configurable y nunca se asigna una palabra reservada (`api`, `error`, `v3`, `swagger`).

**Preguntas al Cliente:**
- ¿Hay preferencia sobre los caracteres permitidos?
- ¿Se aceptan mayúsculas en los códigos?

**Respuesta del Cliente:**

**Nota:** la Etapa 1 había descartado los códigos secuenciales porque permiten adivinar enlaces ajenos probando códigos consecutivos ([ADR-0016](../../adr/0016-formato-del-alias.md)). El nuevo requerimiento lo pide explícitamente, así que se acepta ese riesgo.

---

## Camino 2 – Usuarios logueados

### E3 – Usuario logueado que acorta sin espacio

**Preguntas al Cliente:**
- Un usuario logueado, ¿puede también acortar de forma anónima (`/1`)?
- Si todavía no eligió su espacio, ¿se le impide acortar o acorta como anónimo?

**Propuesta del equipo:** el usuario logueado siempre acorta en su espacio, y sin espacio elegido se le pide que lo elija antes de acortar.

**Respuesta del Cliente:**

### E4 – Largo y nombres permitidos

**Preguntas al Cliente:**
- ¿Qué largo mínimo y máximo tienen el espacio de nombres y el código personalizado?
- ¿Hay nombres prohibidos? Algunos chocan con rutas del sistema (`api`, `assets`, `swagger-ui`, `v3`, `login`, `oauth2`).

**Propuesta del equipo:** espacio de 3 a 30 caracteres y código personalizado de 1 a 64, con una lista configurable de nombres reservados.

**Respuesta del Cliente:**

### E5 – Mayúsculas al abrir un enlace

**Pregunta al Cliente:** si alguien abre `/Melina/1`, ¿se redirige como `/melina/1` o se responde que no existe?

**Propuesta del equipo:** se normaliza a minúsculas y redirige.

**Respuesta del Cliente:**

### E6 – Enlaces al cambiar de espacio

**Requerimiento:** al cambiar el espacio, el anterior queda libre y sus enlaces dejan de funcionar.

**Pregunta al Cliente:** ¿los enlaces vigentes se pierden definitivamente, o deberían pasar al espacio nuevo?

**Propuesta del equipo:** se pierden. Ni el espacio nuevo ni otro usuario que tome el nombre anterior heredan esos enlaces.

**Respuesta del Cliente:**

### E7 – Código personalizado ocupado

**Pregunta al Cliente:** si el código pedido está en uso por un enlace vigente, ¿se rechaza con un mensaje o se asigna otro?

**Propuesta del equipo:** se rechaza con un mensaje claro y no se crea el enlace.

**Respuesta del Cliente:**

### E8 – Autorización en el login con QR

**Requerimiento:** el usuario escanea el QR desde el celular y autoriza explícitamente el inicio de sesión en la computadora.

**Pregunta al Cliente:** para saber quién autoriza, el celular necesita una sesión. ¿Es aceptable que el usuario inicie sesión con Google en el celular la primera vez?

**Propuesta del equipo:** sí. El celular muestra "¿Autorizar el inicio de sesión en la computadora?" con los botones Autorizar y Rechazar, y el pedido vence a los pocos minutos.

**Respuesta del Cliente:**

### E9 – Entorno de la demostración

**Contexto:** Google solo acepta volver a direcciones con HTTPS o a `http://localhost`. Con `localhost` el celular no puede llegar al servidor, así que el login con QR no se podría mostrar.

**Pregunta al Cliente:** ¿la demostración se hace en un servidor con dominio y HTTPS, o se acepta otra forma?

**Respuesta del Cliente:**

### E10 – Supuestos de la Etapa 1

Siguen pendientes de confirmación los supuestos D2 a D8 de la [minuta 01](../../etapa-1/minutas/minuta-01-elucidacion.md). D1 queda reemplazado por el requerimiento de códigos incrementales.

**Respuesta del Cliente:**

---

## Criterios aprobados por el equipo (2026-10-25)

Antes de implementar el Camino 2, el equipo aprobó estos criterios. Se mantienen hasta que el Cliente responda; si responde distinto, se registra en un ADR nuevo.

| Punto | Criterio aprobado | Origen |
|---|---|---|
| E3 | Con sesión iniciada, los enlaces se crean siempre dentro del espacio del usuario | Equipo |
| — | Cada usuario tiene un único espacio, y el espacio es único entre todos los usuarios | Requerimiento del Cliente |
| E5 | Los espacios se normalizan y almacenan en minúsculas: `Melina`, `MELINA` y `melina` son el mismo espacio | Requerimiento del Cliente, aplicado también al buscar |
| — | Espacio: letras minúsculas y números | Requerimiento del Cliente |
| — | Código personalizado: letras minúsculas, números, `-` y `_`; sin espacios ni caracteres que generen problemas en la URL | Requerimiento del Cliente |
| E7 | Un código personalizado ocupado dentro del espacio se rechaza con un mensaje claro | Equipo |
| — | Los códigos pueden repetirse entre usuarios distintos (`ana/1` y `melina/1`) | Requerimiento del Cliente |
| E4 | El Cliente **no definió** largos mínimos ni máximos. El equipo fija máximos **técnicos** de 15 caracteres para el espacio y para el código personalizado, sin mínimo, y una lista de nombres reservados por choque con rutas del sistema. No son requisitos funcionales | Decisión técnica del equipo ([ADR-0029](../../adr/0029-limites-tecnicos-de-longitud.md)) |
| — | Se agrega Spring Security para implementar el login con Google | Equipo |

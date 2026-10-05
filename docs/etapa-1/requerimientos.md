# Requerimientos – Etapa 1

Fuente: [consigna](../consigna.md), §2. Las ambigüedades se resolvieron con supuestos del equipo (ADR 0016 a 0023, pendientes de confirmación del Cliente en la [minuta 01](minutas/minuta-01-elucidacion.md)).

## 1. Requerimientos funcionales

| ID | Requerimiento | Origen |
|---|---|---|
| RF-01 | Recibir una URL original y validarla | Consigna §2.1 |
| RF-02 | Generar un alias único y una URL corta `http://{dominio_o_ip}/{alias}` | Consigna §2.1 |
| RF-03 | Redirigir de forma transparente de la URL corta a la URL original | Consigna §2.2 |
| RF-04 | Página web con el campo "dirección a acortar" y el botón **ACORTAR** | Consigna §2.1 |
| RF-05 | Extensión para Chrome y Firefox que, con un solo clic, acorte la página actual | Consigna §2.1 |
| RF-06 | La web y la extensión muestran la URL corta y un código QR que la codifica | Consigna §2.1 |
| RF-07 | Persistir los enlaces con JPA/Hibernate sobre una base relacional | Consigna §2.3 |

## 2. Reglas de negocio

| ID | Regla | Origen |
|---|---|---|
| RN-01 | El alias es aleatorio, de 5 caracteres, con el alfabeto `23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz`. Distingue mayúsculas de minúsculas | Consigna y ADR-0016 |
| RN-02 | El alias es único: no puede haber dos registros con el mismo alias | Consigna y ADR-0021 |
| RN-03 | El alias nunca coincide con una palabra reservada (`api`, `error`, `v3`, `swagger`) | ADR-0011 |
| RN-04 | Un enlace es **vigente** durante 60 minutos desde su creación. Luego está **vencido** | Consigna §2.1 |
| RN-05 | Un enlace vencido no redirige | Consigna §2.1 |
| RN-06 | El alias de un enlace vencido puede reasignarse a una URL nueva, reutilizando su registro (sin historial) | Consigna y ADR-0021 |
| RN-07 | Cada pedido de acortamiento genera un alias nuevo, aunque la URL ya tenga un enlace vigente | ADR-0017 |
| RN-08 | Una URL es válida si no está vacía, tiene hasta 2048 caracteres, es absoluta, tiene esquema `http` o `https` y tiene host. No se verifica que exista | ADR-0020 |
| RN-09 | Se rechaza una URL que pertenece al propio servicio (mismo esquema, host y puerto que la URL base) | ADR-0019 |

## 3. Requerimientos no funcionales y restricciones

| ID | Requerimiento | Origen |
|---|---|---|
| RNF-01 | Backend en Java con Spring Boot que expone una API REST | Consigna |
| RNF-02 | La web y la extensión consumen el mismo backend, y la lógica de negocio reside en el backend | Consigna §2.4 |
| RNF-03 | Contrato de la API definido en OpenAPI antes de implementar | Consigna §3 y ADR-0008 |
| RNF-04 | Tests unitarios y de integración con medición de cobertura | Consigna §3.4 y ADR-0009 |
| RNF-05 | La URL base del servicio es configurable, sin estar fija en el código | ADR-0022 |
| RNF-06 | El diseño debe tolerar cambios de requerimientos en etapas posteriores | Consigna §1 |

## 4. Historias de usuario y criterios de aceptación

### HU-01 – Acortar una URL desde la página web
**Como** usuario, **quiero** pegar una URL larga en una página y presionar ACORTAR **para** obtener una URL corta que pueda compartir.

| CA | Criterio |
|---|---|
| CA-01.1 | **Dado** que ingreso una URL válida, **cuando** presiono ACORTAR, **entonces** veo la URL corta con el formato `{URL base}/{alias}`, con un alias de 5 caracteres del alfabeto definido. |
| CA-01.2 | **Dado** que ingreso una URL vacía, mayor a 2048 caracteres, sin esquema `http`/`https` o sin host, **cuando** presiono ACORTAR, **entonces** veo un mensaje de error y no se crea ningún enlace. |
| CA-01.3 | **Dado** que ingreso una URL corta del propio servicio, **cuando** presiono ACORTAR, **entonces** veo un mensaje de error y no se crea ningún enlace. |
| CA-01.4 | **Dado** que acorto dos veces la misma URL, **cuando** obtengo ambos resultados, **entonces** los alias son distintos. |
| CA-01.5 | **Dado** que obtuve una URL corta, **entonces** el enlace queda guardado en la base de datos y sigue vigente aunque se reinicie el servidor dentro de los 60 minutos. |

### HU-02 – Acortar la página actual desde la extensión
**Como** usuario de Chrome o Firefox, **quiero** tocar el ícono de la extensión **para** obtener la URL corta de la página que estoy viendo sin copiar ni pegar nada.

| CA | Criterio |
|---|---|
| CA-02.1 | **Dado** que estoy en una página con URL `http`/`https` y la extensión está configurada, **cuando** toco el ícono, **entonces** veo la URL corta de esa página y su QR, sin otra acción. |
| CA-02.2 | **Dado** que estoy en una página que no se puede acortar (por ejemplo `chrome://` o `about:`), **cuando** toco el ícono, **entonces** veo un mensaje de error. |
| CA-02.3 | **Dado** que el backend no está disponible o la extensión no tiene el permiso, **cuando** toco el ícono, **entonces** veo un mensaje que lo indica. |
| CA-02.4 | La extensión funciona en Chrome y en Firefox con el mismo código. |

### HU-03 – Configurar la extensión
**Como** usuario, **quiero** indicar la dirección del servidor en la extensión **para** usarla en cualquier red.

| CA | Criterio |
|---|---|
| CA-03.1 | **Dado** que estoy en la pantalla de opciones, **cuando** ingreso la dirección del servidor y presiono Guardar, **entonces** el navegador me pide permiso para ese host y, si lo acepto, la dirección queda guardada. |
| CA-03.2 | **Dado** que rechazo el permiso, **entonces** veo un mensaje indicando que la extensión no podrá conectarse. |

### HU-04 – Acceder a una URL corta
**Como** persona que recibió una URL corta, **quiero** abrirla **para** llegar a la URL original.

| CA | Criterio |
|---|---|
| CA-04.1 | **Dado** un enlace vigente, **cuando** accedo a su URL corta, **entonces** el servidor responde 302 con `Location` igual a la URL original y el navegador me lleva a ella. |
| CA-04.2 | **Dado** un enlace creado hace 60 minutos o más, **cuando** accedo a su URL corta, **entonces** no se redirige y veo una página que indica que el enlace no está disponible o venció (HTTP 404). |
| CA-04.3 | **Dado** un alias que no existe, **cuando** accedo a la URL corta, **entonces** veo la misma página (HTTP 404). |
| CA-04.4 | Las direcciones de la página web, la API y Swagger siguen funcionando: no se interpretan como alias. |

### HU-05 – Reasignar un alias vencido
**Como** responsable del servicio, **quiero** que los alias vencidos puedan volver a usarse **para** mantener los alias cortos.

| CA | Criterio |
|---|---|
| CA-05.1 | **Dado** un enlace vencido con alias `X`, **cuando** el generador produce `X` para un pedido nuevo, **entonces** `X` pasa a redirigir a la nueva URL durante 60 minutos. |
| CA-05.2 | **Dado** un enlace vigente con alias `X`, **cuando** el generador produce `X` para un pedido nuevo, **entonces** se genera otro alias y `X` sigue redirigiendo a su URL original. |

### HU-06 – Ver el código QR
**Como** usuario, **quiero** ver un código QR de la URL corta **para** que otra persona pueda abrirla escaneándolo.

| CA | Criterio |
|---|---|
| CA-06.1 | **Dado** que acorté una URL desde la web o la extensión, **entonces** veo un QR junto a la URL corta. |
| CA-06.2 | **Dado** que escaneo el QR con un celular, **entonces** obtengo exactamente la URL corta, y al abrirla llego a la URL original si el enlace está vigente. |
| CA-06.3 | **Dado** un alias vencido o inexistente, **cuando** se pide su QR a la API, **entonces** responde 404. |

## 5. Trazabilidad

| Historia | Requerimientos y reglas | Incremento |
|---|---|---|
| HU-01 | RF-01, RF-02, RF-04, RF-07, RN-01 a RN-03, RN-07 a RN-09 | 3 (API) y 6 (web) |
| HU-02 | RF-05, RNF-02 | 7 |
| HU-03 | RNF-05 | 7 |
| HU-04 | RF-03, RN-04, RN-05 | 4 |
| HU-05 | RN-06 | 4 |
| HU-06 | RF-06 | 5, 6 y 7 |

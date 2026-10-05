# Minuta 01 – Elucidación de requerimientos (Etapa 1)

| | |
|---|---|
| **Proyecto** | Servicio de Acortamiento y Gestión de Enlaces (Link Shortener) |
| **Cátedra** | Paradigmas de Programación VI |
| **Etapa** | 1 |
| **Fecha** | ____ / ____ / ______ |
| **Cliente** | ______________________ (docente) |
| **Equipo** | ______________________ |

## Objetivo de la reunión

Resolver con el Cliente los puntos de la Etapa 1 que la consigna no define, antes de cerrar el diseño (modelo de dominio y contrato de la API REST).

Para cada punto se indica qué dice la consigna, las opciones que el equipo identificó y la propuesta del equipo. **La propuesta no se implementa hasta que el Cliente la confirme o la cambie.**

---

## D1 – Formato y largo del alias

**Consigna:** el alias debe ser único y *"tan corto y memorizable como sea posible"*. Da como ejemplos `xT3se` (alfanumérico) y `15321` (numérico).

**Opciones:**
- a) Numérico secuencial (`15321`).
- b) Numérico aleatorio.
- c) Alfanumérico aleatorio (`xT3se`).

**Propuesta del equipo:** alfanumérico aleatorio, sin caracteres que se confunden (`0/O`, `1/l/I`), con largo configurable (inicial: 5).
Un alias secuencial permite adivinar los enlaces de otros usuarios probando números consecutivos, y estos enlaces pueden apuntar a carpetas privadas (por ejemplo, de Google Drive).

**Preguntas al Cliente:**
- ¿Hay preferencia entre numérico y alfanumérico?
- ¿Se distinguen mayúsculas de minúsculas? (Más combinaciones con el mismo largo, pero es más difícil de dictar o recordar.)
- ¿Hay un largo máximo esperado?

**Respuesta del Cliente:**

> 

---

## D2 – Acortar nuevamente una URL que ya tiene un enlace vigente

**Consigna:** no lo especifica.

**Opciones:**
- a) Generar un alias nuevo cada vez.
- b) Devolver el alias vigente existente, sin cambiar su vencimiento.
- c) Devolver el alias vigente existente y renovar sus 60 minutos.

**Propuesta del equipo:** a) alias nuevo cada vez. Cada pedido tiene sus propios 60 minutos de vigencia y el pedido de un usuario no afecta los enlaces de otro.

**Respuesta del Cliente:**

> 

---

## D3 – Acceso a un enlace vencido o inexistente

**Consigna:** pasados 60 minutos el enlace *"dejará de redireccionar"*. No indica qué debe ver el usuario.

**Opciones:**
- a) Página HTML informativa ("enlace no disponible o vencido").
- b) Respuesta de error sin página (solo código HTTP).
- c) Redirigir a la página principal del acortador.

**Propuesta del equipo:** a) página HTML simple con código HTTP 404, tanto para vencidos como para inexistentes. Quien accede es una persona desde el navegador o al escanear el QR.

**Preguntas al Cliente:**
- ¿Debe distinguirse "vencido" de "inexistente" para el usuario?

**Respuesta del Cliente:**

> 

---

## D4 – Acortar una URL que ya es un enlace corto del propio servicio

**Consigna:** no lo especifica.

**Opciones:**
- a) Rechazarla.
- b) Aceptarla (enlace corto que apunta a otro enlace corto).

**Propuesta del equipo:** a) rechazarla, para evitar cadenas o bucles de redirección.

**Respuesta del Cliente:**

> 

---

## D5 – Criterio de "URL original válida"

**Consigna:** exige una *"URL original válida"* sin definir el criterio.

**Propuesta del equipo:**
- URL absoluta con host.
- Solo esquemas `http` y `https`.
- Largo máximo de 2048 caracteres.

**Preguntas al Cliente:**
- ¿Se aceptan otros esquemas (por ejemplo `ftp`, `mailto`)?
- ¿Debe verificarse que la URL exista o responda, o alcanza con que esté bien formada?

**Respuesta del Cliente:**

> 

---

## D6 – Historial de enlaces vencidos

**Consigna:** el alias vencido *"quedará disponible y podrá ser reasignado"*. No indica si debe conservarse el registro anterior.

**Opciones:**
- a) Sin historial: al reasignar el alias se reemplaza el registro anterior.
- b) Con historial: se conservan todos los enlaces generados, aun vencidos.

**Propuesta del equipo:** a) sin historial en la Etapa 1, porque la consigna no lo requiere.

**Respuesta del Cliente:**

> 

---

## D7 – Dominio o IP del enlace corto

**Consigna:** formato `http://{dominio_o_ip}/{alias}`, con el ejemplo `http://192.168.1.50/xT3se`.

**Propuesta del equipo:**
- El dominio o IP es configurable en el servidor, sin estar fijo en el código.
- La extensión tiene una pantalla de opciones para indicar la dirección del servidor.

**Preguntas al Cliente:**
- ¿En qué entorno se va a demostrar (red local, IP fija, dominio)?
- ¿Se requiere HTTPS en la demostración?

**Respuesta del Cliente:**

> 

---

## D8 – Generación del código QR

**Consigna:** *"tanto la página web como el complemento deben generar y mostrar un código QR"*.

**Opciones:**
- a) Lo genera el servidor y la página web y la extensión lo muestran.
- b) Lo genera cada cliente (página web y extensión) con una librería propia.

**Propuesta del equipo:** a) generación en el servidor. Hay una única implementación, se puede probar automáticamente y la página web y la extensión quedan más simples.

**Preguntas al Cliente:**
- ¿La consigna exige que el QR se genere en el cliente, o alcanza con que ambos lo muestren?

**Respuesta del Cliente:**

> 

---

## Otros temas planteados por el Cliente

> 

## Acuerdos y próximos pasos

- [ ] Registrar las respuestas como decisiones de diseño (ADR).
- [ ] Actualizar requerimientos, historias de usuario y criterios de aceptación de la Etapa 1.
- [ ] Definir el contrato de la API REST (OpenAPI) en función de los acuerdos.

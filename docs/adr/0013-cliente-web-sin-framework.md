# ADR-0013 – Página web en HTML, CSS y JS sin framework, servida por Spring Boot

- **Estado:** Aceptada
- **Fecha:** 2026-10-05
- **Etapa:** 1
- **Origen:** consigna (cliente web) y decisión del equipo

## Contexto
La consigna pide una interfaz web con un campo para la dirección a acortar, un botón **ACORTAR** y la visualización de la URL corta y el QR. Es una sola pantalla con un formulario.

## Decisión
- **HTML, CSS y JavaScript sin framework** (`fetch` para consumir la API).
- El código fuente vive en `frontend/`. Al compilar, Gradle lo copia a los recursos estáticos del backend y **Spring Boot lo sirve** desde la raíz.

## Alternativas consideradas
- **React, Angular u otro framework:** agrega build, dependencias y complejidad sin necesidad para una pantalla.
- **Servidor web aparte para el frontend:** obliga a configurar CORS y a desplegar dos piezas.

## Consecuencias
- La web y la API comparten origen, así que no hace falta CORS.
- Hay una sola pieza para levantar y desplegar.
- La lógica de negocio queda en el backend: la página solo envía la URL y muestra el resultado.
- Si una etapa futura requiere una interfaz más compleja, se puede reevaluar con un nuevo ADR.

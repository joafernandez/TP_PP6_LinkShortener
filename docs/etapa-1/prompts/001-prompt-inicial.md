# 001 – Prompt inicial

- **Fecha:** 2026-10-05
- **Herramienta:** Claude Code (VS Code)
- **Objetivo:** dar a la IA el contexto del TP, los lineamientos técnicos y la forma de trabajo, y obtener un análisis de la Etapa 1 sin generar código.

## Prompt (textual)

> Quiero que trabajes conmigo como ingeniero de software en este proyecto. Antes de implementar cualquier cosa, necesito que leas, entiendas y respetes la especificación y los lineamientos técnicos que indico a continuación.
>
> **1. Especificación oficial.** Dentro del proyecto se encuentra `docs/TP_PP6_v1.0.md` *(en el repositorio el archivo se llama `docs/consigna.md`)*. Leé ese archivo completo antes de hacer cualquier propuesta o modificación. Ese documento es la fuente oficial de requerimientos. Actualmente vamos a trabajar solamente sobre la Etapa 1. No inventes requerimientos de las Etapas 2 y 3 ni intentes anticiparlos. Sí debemos procurar que el diseño tenga bajo acoplamiento, buena cohesión y pueda evolucionar sin reescrituras masivas, porque los requerimientos cambiarán posteriormente.
>
> **2. Objetivo actual.** La Etapa 1 consiste en desarrollar un servicio de acortamiento y gestión de URLs (Link Shortener). Debe contemplar, como mínimo, lo especificado en el TP:
> - recibir una URL original válida;
> - generar un alias corto y único;
> - generar una URL corta;
> - permitir acceder a esa URL corta y redireccionar a la URL original;
> - la URL corta debe tener una vigencia de 60 minutos;
> - una vez vencida debe dejar de redireccionar;
> - el alias vencido debe poder quedar disponible para reutilización;
> - persistir los datos mediante JPA/Hibernate sobre una base de datos relacional;
> - disponer de una interfaz web con un campo para ingresar la URL y un botón ACORTAR;
> - mostrar como resultado la URL corta y un código QR;
> - disponer además de una extensión para Chrome y Firefox;
> - la extensión debe poder tomar la URL de la página que el usuario está visitando y, mediante una acción simple del usuario, obtener la URL corta y mostrar también el QR;
> - la interfaz web y la extensión deben consumir el mismo backend;
> - el backend debe exponer una API REST.
>
> **3. Lineamientos tecnológicos.** El stack obligatorio del proyecto es: Java, Spring Boot, JPA/Hibernate, API REST, cliente web. Para mantener coherencia con el entorno de desarrollo que ya utilizamos, quiero partir además de estos lineamientos: Java 21, Gradle, Spring Boot, Spring Web, JPA con Hibernate, HSQLDB como base de datos relacional para esta primera versión, EntityManager para acceso a persistencia y consultas, capa Service para lógica de negocio, @RestController para la API REST, HQL/JPQL cuando sea necesario consultar mediante JPA. Inicialmente no agregues una capa Repository. Si considerás que existe una razón técnica importante para introducirla, explicala primero y esperá aprobación antes de hacerlo. Tampoco agregues Docker, Redis, PostgreSQL, React, Angular, microservicios, autenticación u otras tecnologías que no sean necesarias para cumplir esta etapa, salvo que primero justifiques claramente su necesidad. Para el cliente web, priorizá inicialmente una solución sencilla y mantenible. No elijas un framework frontend solamente por preferencia técnica.
>
> **4. Estructura actual.** El workspace está ubicado en `C:\TP_PP6_LinkShortener`, con las carpetas `backend/`, `database/`, `docs/`, `extension/` y `frontend/`. Las carpetas están prácticamente vacías. No asumas que existe código previo.
>
> **5. Forma de trabajo.** Quiero desarrollar el proyecto incrementalmente, no generar todo de una sola vez. Cada cambio importante debe seguir este criterio: analizar → proponer → revisar → implementar → probar. No hagas modificaciones masivas sin explicar antes qué vas a tocar. No borres ni reemplaces archivos sin justificarlo. No inventes endpoints, entidades, campos o reglas que no surjan de la consigna o de decisiones que tomemos explícitamente. Cuando exista una ambigüedad funcional, señalala como una pregunta o decisión pendiente en lugar de resolverla arbitrariamente. La arquitectura debe ser suficientemente simple para la Etapa 1, pero debe evitar acoplar innecesariamente la lógica del acortamiento a la interfaz web o a la extensión. La lógica de negocio del acortador debe residir en el backend. Tanto la página web como la extensión deben ser clientes del mismo servicio.
>
> **6. Lo que quiero que hagas AHORA.** No escribas código y no crees ni modifiques archivos todavía. Primero:
> 1. Leé completamente la consigna.
> 2. Inspeccioná la estructura actual del workspace.
> 3. Extraé los requerimientos correspondientes únicamente a la Etapa 1.
> 4. Separalos en: requerimientos funcionales; reglas de negocio; restricciones tecnológicas; puntos ambiguos o decisiones pendientes.
> 5. Proponé una arquitectura inicial sencilla para: backend; persistencia; interfaz web; extensión Chrome/Firefox; generación del QR; redirección de URLs.
> 6. Proponé, conceptualmente y sin crear código todavía: modelo de datos mínimo; responsabilidades de las principales clases/capas; endpoints REST necesarios; flujo para crear un enlace corto; flujo para resolver/redireccionar un enlace; forma de controlar la expiración de 60 minutos y reutilización del alias.
> 7. Indicá las dudas que deberíamos resolver antes de comenzar a programar.
> 8. Finalmente, proponé cuál debería ser el primer incremento de implementación, pequeño y verificable.
>
> Detenete ahí y esperá mi aprobación. No implementes todavía.

## Resultado

- Requerimientos de la Etapa 1 separados en funcionales, reglas de negocio y restricciones.
- Arquitectura en capas (Controller → Service → EntityManager), con la web y la extensión como clientes del mismo backend.
- Identificación de 10 dudas (D1 a D10), entre ellas el formato del alias, el comportamiento ante enlaces vencidos y dónde generar el QR.
- Señalamiento de que la redirección debe ser 302 y no 301, porque los alias se reasignan.

## Correcciones a partir del resultado

- El equipo **rechazó** que la IA propusiera valores por defecto para las ambigüedades funcionales (D1 a D8): debían quedar pendientes y llevarse al Cliente.
- No fijar Java 21: verificar primero el JDK instalado (resultó ser el JDK 25).
- Paquete raíz `app`, coherente con el proyecto de referencia.

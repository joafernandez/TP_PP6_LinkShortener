# ADR-0010 – Puntos de extensión: `AliasGenerator`, propiedades configurables y `Clock`

- **Estado:** Aceptada
- **Fecha:** 2026-10-05
- **Etapa:** 1
- **Origen:** consigna (extensibilidad y tolerancia al cambio) y decisión del equipo

## Contexto
La consigna advierte que el Cliente cambiará los requerimientos sin aviso previo y evalúa si el diseño los soporta sin reescrituras. No se deben anticipar requerimientos, pero sí aislar lo que **la propia consigna muestra que puede variar**:
- El formato del alias: la consigna da ejemplos de dos formatos distintos.
- Parámetros como la vigencia de 60 minutos o el dominio.
- El paso del tiempo, del que depende la regla de vencimiento.

## Decisión
- **`AliasGenerator`** como interfaz, con una implementación concreta. La estrategia (formato y largo) se define cuando el Cliente responda D1.
- **`@ConfigurationProperties`** para los parámetros: URL base del servicio, duración de la vigencia, largo del alias y palabras reservadas.
- **`java.time.Clock`** inyectado como bean: el código obtiene la hora actual desde el `Clock` y nunca con `Instant.now()` directo.
- No se agregan interfaces "por las dudas" en services u otras clases: solo en los puntos de variación identificados.

## Alternativas consideradas
- **Valores fijos en el código:** cualquier cambio obliga a recompilar y modificar la lógica.
- **Interfaces para todos los services:** más abstracción, sin un beneficio concreto hoy.

## Consecuencias
- Cambiar la estrategia de alias implica una implementación nueva, sin tocar el service.
- Cambiar la vigencia o el dominio es solo una cuestión de configuración.
- Con un `Clock` fijo, los tests verifican el vencimiento de 60 minutos sin esperar.

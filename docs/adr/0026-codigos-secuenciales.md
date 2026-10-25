# ADR-0026 – Códigos secuenciales: el menor código libre

- **Estado:** Aceptada (supuesto del equipo, pendiente de confirmación del Cliente – E1 y E2)
- **Fecha:** 2026-10-25
- **Etapa:** 2
- **Origen:** requerimiento del Cliente para la Etapa 2 y decisión del equipo ante sus ambigüedades ([minuta 02](../etapa-2/minutas/minuta-02-elucidacion.md), E1 y E2)
- **Reemplaza a:** [ADR-0016](0016-formato-del-alias.md)

## Contexto
El Cliente pide que el código tenga **1 carácter siempre que sea posible**, que se asigne de forma **incremental** (`/1`, `/2`, `/3`), que los códigos de enlaces expirados se **reutilicen** y que, al agotarse los de 1 carácter, se pase a 2 caracteres. Esto contradice el alias aleatorio de 5 caracteres de la Etapa 1.

## Decisión
- Se asigna el **menor código libre** en orden *shortlex*: primero por largo y, entre los del mismo largo, por el orden del alfabeto.
- Un código está libre si no tiene un enlace **vigente**. Los vencidos se reutilizan con la reasignación atómica de [ADR-0021](0021-sin-historial-de-enlaces.md).
- Alfabeto configurable (`app.alias.alphabet`), con este valor por defecto (33 símbolos, sin `0`, `o` ni `l`):
  `123456789abcdefghijkmnpqrstuvwxyz`
- La secuencia es `1` … `9`, `a` … `z`, `11`, `12` … `1z`, `21` … y así sucesivamente, sin un largo fijo.
- Nunca se asigna una palabra reservada ([ADR-0011](0011-ruta-del-alias-y-palabras-reservadas.md)): se saltea y se toma la siguiente.
- La ruta `/{alias:[A-Za-z0-9]+}` no cambia: el nuevo alfabeto está incluido en ella.

## Alternativas consideradas
- **Contador que avanza y reutiliza al dar la vuelta:** pasa a 2 caracteres aunque haya códigos de 1 carácter libres.
- **Solo dígitos:** apenas 9 códigos de 1 carácter.
- **Dígitos, minúsculas y mayúsculas:** más códigos cortos, pero más difíciles de dictar, e incoherentes con el espacio de nombres en minúsculas del Camino 2.

## Consecuencias
- 33 códigos de 1 carácter y 1089 de 2. Con 60 minutos de vigencia, los enlaces activos casi siempre usan 1 o 2 caracteres.
- Los códigos son **predecibles**: se pueden recorrer enlaces ajenos probando códigos consecutivos. La Etapa 1 lo había descartado por eso, pero ahora es un requerimiento explícito del Cliente.
- El largo deja de ser un parámetro: `app.alias.length` pierde sentido. Se decide al implementar si se elimina o se reinterpreta.
- El mecanismo de generación cambia de contrato: ver [ADR-0027](0027-generador-de-codigos-con-codigos-vigentes.md).

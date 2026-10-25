# ADR-0027 – El generador de códigos recibe los códigos vigentes

- **Estado:** Aceptada
- **Fecha:** 2026-10-25
- **Etapa:** 2
- **Origen:** decisión del equipo, derivada de [ADR-0026](0026-codigos-secuenciales.md)
- **Modifica:** el contrato de `GeneradorAlias` definido en [ADR-0010](0010-puntos-de-extension.md)

## Contexto
Hoy `GeneradorAlias.generar()` devuelve un candidato aleatorio sin conocer el estado de la base, y el service reintenta si está ocupado. Para elegir el **menor código libre** el generador necesita saber cuáles están ocupados: con candidatos aleatorios no se puede garantizar que se use el más corto.

## Decisión
- `GeneradorAlias` sigue siendo la interfaz del punto de extensión, pero recibe el conjunto de códigos **vigentes** y devuelve el primero libre en el orden definido por la estrategia, salteando las palabras reservadas.
- El generador sigue sin acceder a la base: el service obtiene los códigos vigentes del Repository y se los pasa.
- La unicidad la sigue garantizando la base (`UNIQUE` del alias). Si dos pedidos simultáneos eligen el mismo código, uno falla con `ColisionAliasException`, el service vuelve a leer los vigentes y prueba de nuevo, con el máximo de intentos actual (`app.alias.max-attempts`).
- Cada intento sigue corriendo en su propia transacción, como en la Etapa 1.
- El mismo generador servirá para el espacio de cada usuario del Camino 2, recibiendo los códigos vigentes de ese espacio.

## Alternativas consideradas
- **Calcular el código libre con una consulta SQL:** acopla la estrategia a la base y la hace difícil de testear.
- **Mantener `generar()` y probar candidatos en orden contra la base:** una consulta por candidato, y el service tendría que conocer el orden.
- **Una secuencia de la base de datos:** no permite reutilizar los códigos vencidos.

## Consecuencias
- Cambiar la estrategia sigue siendo una implementación nueva, sin tocar el service.
- El generador se testea con conjuntos en memoria, sin base de datos.
- Cada creación lee los códigos vigentes. Con una vigencia de 60 minutos el conjunto es chico, y alcanza para la escala del proyecto.
- La implementación aleatoria (`GeneradorAliasAleatorio`) deja de cumplir el requerimiento. Se decide al implementar si se elimina o se adapta.

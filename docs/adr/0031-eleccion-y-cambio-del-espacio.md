# ADR-0031 – Elección y cambio del espacio de nombres

- **Estado:** Aceptada
- **Fecha:** 2026-10-25
- **Etapa:** 2
- **Origen:** requerimiento del Cliente para la Etapa 2 (Camino 2) y criterios aprobados por el equipo ([minuta 02](../etapa-2/minutas/minuta-02-elucidacion.md))

## Contexto
Después del primer login, el usuario elige su espacio de nombres antes de acortar como usuario registrado. Puede cambiarlo por otro disponible: el anterior queda libre y sus enlaces dejan de funcionar. El modelo y la unicidad en la base están en [ADR-0028](0028-modelo-de-usuarios-y-espacios.md); los límites técnicos, en [ADR-0029](0029-limites-tecnicos-de-longitud.md).

## Decisión
- Un único endpoint, `PUT /api/v1/me/namespace` con `{"namespace": "..."}`, para elegir y para cambiar: el usuario siempre tiene a lo sumo un espacio, así que se trata como el reemplazo de un valor. Responde 200 con el nombre guardado, 400 si es inválido, 409 si está ocupado y 401 sin sesión. Requiere el token CSRF.
- `ValidadorEspacio` normaliza a minúsculas y aplica las reglas en este orden, con un mensaje claro para cada una: no vacío; solo letras sin acentos y números; hasta 15 caracteres; no reservado.
- Los nombres reservados son configurables en `app.namespace.reserved` (`api`, `assets`, `v3`, `error`, `login`, `logout`, `oauth2`, `swagger`) y se comparan en minúsculas.
- `EspacioService.elegir`: si el usuario no tiene espacio, lo crea; si lo tiene, **renombra la misma fila**; si el nombre no cambia, no hace nada. El nombre anterior queda libre en el mismo momento, porque ninguna fila lo conserva.
- La disponibilidad no se consulta antes de guardar: la decide la restricción `UNIQUE` de la base, que `EspacioRepository` traduce a `EspacioOcupadoException` (409). Así funciona igual con pedidos simultáneos.

## Alternativas consideradas
- **`POST` para elegir y `PATCH` para cambiar:** dos endpoints para la misma operación sobre un valor único.
- **Borrar el espacio y crear otro al cambiar:** cambiaría el `id` del espacio, y en el paso 2.4 los enlaces apuntarán a él.
- **Consultar la disponibilidad antes de guardar:** entre la consulta y el guardado otro usuario puede tomar el nombre; igual hay que depender de la base.

## Consecuencias
- Como se renombra la misma fila, en el paso 2.4 los enlaces del espacio seguirían asociados a él con el nombre nuevo. Para cumplir que **los enlaces del espacio anterior dejen de funcionar**, el cambio de nombre deberá vencer en la misma transacción los enlaces vigentes del espacio. Se implementa y se testea en el paso 2.4, cuando existan esos enlaces.
- La web muestra la advertencia antes de cambiar el espacio.

# ADR-0034 – Transición de 60 minutos para los nombres de espacio anteriores

- **Estado:** Aceptada
- **Fecha:** 2026-11-15
- **Etapa:** 2
- **Origen:** nuevo requerimiento del Cliente y decisiones aprobadas por el equipo (ver [prompt 008](../etapa-2/prompts/008-transicion-del-espacio.md))
- **Reemplaza:** la regla "al cambiar de espacio se vencen los enlaces" de [ADR-0031](0031-eleccion-y-cambio-del-espacio.md) y [ADR-0032](0032-enlaces-dentro-del-espacio.md)

## Contexto
Hasta ahora, al cambiar el espacio, los enlaces del anterior dejaban de funcionar al instante. El Cliente cambió el requerimiento:
- el nombre nuevo funciona en el acto y **todos los enlaces existentes funcionan con él**;
- el nombre anterior sigue funcionando **60 minutos desde el cambio**, con los mismos enlaces, y queda **reservado**: nadie más puede tomarlo en ese plazo;
- el cambio **no renueva** la vigencia de los enlaces: el vencimiento del enlace (60 minutos desde su creación) y el del nombre anterior (60 minutos desde el cambio) son independientes;
- la redirección sigue siendo un 302 directo, sin pantalla intermedia;
- el dueño ve en «Tu espacio» cuánto tiempo real le queda a cada nombre anterior.

Decisiones del equipo sobre los casos no cubiertos:
1. **Varios cambios seguidos:** se conservan todos los nombres anteriores, cada uno con su propio vencimiento, contado desde que dejó de ser el principal.
2. **Volver a un nombre anterior:** el dueño puede recuperar un nombre que todavía tiene reservado. Vuelve a ser el principal y deja de vencer; el que abandona empieza su propia transición; los demás conservan su vencimiento.
3. **QR:** el QR de un enlace con el nombre anterior sigue funcionando mientras el nombre y el enlace estén vigentes, y no se regenera con el nombre nuevo.

## Decisión
- **Tabla de nombres `namespace_name`** (migración `V5`): `name` con `UNIQUE`, el espacio al que pertenece y `expires_at`. El nombre principal tiene `expires_at` nulo; los anteriores, la fecha en que se liberan. Como **todos** los nombres (principales y en transición) están en la misma columna `UNIQUE`, la base impide que dos espacios tengan o reserven el mismo nombre, aunque lleguen pedidos simultáneos (regla 5).
- `namespace.name` sigue guardando el nombre principal, así que el resto del código y las restricciones de [ADR-0028](0028-modelo-de-usuarios-y-espacios.md) no cambian. La migración copia los nombres actuales a la tabla nueva como principales.
- **Reclamar un nombre** (`NombreDeEspacioRepository`), igual que la reasignación de códigos vencidos ([ADR-0021](0021-sin-historial-de-enlaces.md)):
  - si no existe, se inserta (un choque con el `UNIQUE` se traduce a `EspacioOcupadoException`);
  - si es del mismo espacio y está vigente, vuelve a ser principal;
  - si su transición ya venció, se reasigna con un `UPDATE` condicional (`WHERE expires_at <= ahora`), así que de dos pedidos simultáneos gana uno solo;
  - en cualquier otro caso, está ocupado (409).
- **Al cambiar:** se reclama el nombre nuevo, el principal anterior pasa a vencer en `ahora + app.namespace.transition` (60 minutos por defecto) y `namespace.name` se renombra, todo en una transacción. **Los enlaces no se tocan:** siguen atados al espacio, no al nombre, y conservan su vencimiento (regla 4). No se duplican enlaces ni se generan códigos nuevos (regla 3).
- **Redirección y QR:** `/{espacio}/{código}` busca el espacio por cualquier nombre vigente (principal o en transición) y después el enlace vigente de ese espacio. El QR codifica la URL con el nombre que se pidió, así que no se regenera con el nombre nuevo.
- **API:** `GET /api/v1/me` y `PUT /api/v1/me/namespace` agregan `previousNamespaces` (nombre y `expiresAt`, de los que siguen vigentes). Es un cambio aditivo: los clientes actuales no se rompen.
- **Web:** «Tu espacio» muestra cada nombre anterior con los minutos que le quedan, calculados con el `expiresAt` del servidor, y avisa cuando expira.
- Un nombre vencido queda como fila hasta que alguien lo reclama; no hace falta una tarea de limpieza.

## Alternativas consideradas
- **Guardar el nombre anterior como columna de `namespace`:** dos columnas distintas no pueden compartir una restricción `UNIQUE`, y solo se podría guardar un nombre anterior (contradice la decisión 1).
- **Tabla de nombres anteriores aparte de `namespace.name`:** la unicidad entre las dos tablas no la garantiza la base; habría que consultar antes de guardar, y dos pedidos simultáneos podrían tomar el mismo nombre.
- **Duplicar los enlaces con el nombre nuevo:** contradice la regla 3 y obligaría a mantener dos copias con el mismo vencimiento.

## Consecuencias
- Se dejan de vencer los enlaces al cambiar de espacio: se eliminan `EnlaceDeEspacioRepository.vencerVigentes` y la dependencia de `espacio` hacia `enlace` en `EspacioService`.
- Cuatro tests que verificaban el comportamiento anterior se reescriben para la regla nueva.
- Que un nombre siga reservado 60 minutos impide a otros usarlo en ese plazo, también si el dueño ya no lo quiere.

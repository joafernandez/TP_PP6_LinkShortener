# ADR-0021 – Sin historial: el alias vencido se reasigna reutilizando su registro

- **Estado:** Aceptada (supuesto del equipo, pendiente de confirmación del Cliente – D6)
- **Fecha:** 2026-10-05
- **Etapa:** 1
- **Origen:** decisión del equipo ante una ambigüedad de la consigna ([minuta 01](../etapa-1/minutas/minuta-01-elucidacion.md), D6)

## Contexto
La consigna indica que el alias vencido *"quedará disponible y podrá ser reasignado"*, sin mencionar si debe conservarse el registro anterior.

## Decisión
- **No se conserva historial** de los enlaces vencidos.
- El `alias` tiene una restricción **`UNIQUE`** en la base: hay un solo registro por alias.
- Cuando el generador produce un alias que pertenece a un enlace vencido, ese registro se **actualiza** con la nueva URL y la nueva vigencia. La actualización es atómica y solo procede si el enlace sigue vencido (`... WHERE alias = :alias AND expiresAt < :ahora`).
- Si dos pedidos simultáneos generan el mismo alias, la restricción `UNIQUE` hace fallar a uno de ellos, que reintenta con otro alias.

## Alternativas consideradas
- **Guardar historial:** no permite el `UNIQUE` sobre el alias, porque un alias aparecería en varios registros. La unicidad entre los enlaces vigentes habría que garantizarla por código, que es más complejo y propenso a errores de concurrencia.
- **Borrar periódicamente los vencidos:** no es necesario para cumplir la regla. Puede agregarse más adelante como limpieza.

## Consecuencias
- Modelo simple y unicidad garantizada por la base de datos.
- Se pierde la información de los enlaces anteriores. Si una etapa futura requiere historial o estadísticas, el cambio queda acotado al paquete `app.link`, con una migración nueva y un ADR que reemplace a este.

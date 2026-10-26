# ADR-0028 – Modelo de usuarios y espacios de nombres

- **Estado:** Aceptada
- **Fecha:** 2026-10-25
- **Etapa:** 2
- **Origen:** requerimiento del Cliente para la Etapa 2 (Camino 2) y criterios aprobados por el equipo ([minuta 02](../etapa-2/minutas/minuta-02-elucidacion.md))

## Contexto
Los usuarios inician sesión con Google y eligen un espacio de nombres (`notes.io/melina/1`). Requerimientos del Cliente:
- cada usuario tiene **un único** espacio;
- el espacio es **único** entre todos los usuarios, y la unicidad debe garantizarse **también en la base de datos**;
- se almacena y se trata en **minúsculas**, con letras minúsculas y números;
- el usuario puede cambiarlo por otro disponible, y el anterior queda libre.

## Decisión
- Dos tablas, creadas por la migración `V2`:
  - `app_user`: el usuario de Google, identificado por el `sub` de Google (`UNIQUE`), que no cambia aunque cambie el email. Se guardan también el email, el nombre y la fecha de alta. Se usa `app_user` porque `user` es palabra reservada en SQL.
  - `namespace`: el nombre del espacio y su dueño.
- En `namespace`, la base garantiza las reglas del Cliente:
  - `uk_namespace_name UNIQUE (name)`: dos usuarios no pueden tener el mismo espacio;
  - `uk_namespace_user UNIQUE (user_id)`: un usuario no puede tener dos espacios;
  - `ck_namespace_name CHECK (REGEXP_MATCHES(name, '[a-z0-9]+'))`: solo minúsculas y números, aunque se escriba directo en la base.
- El espacio se crea recién cuando el usuario lo elige: un usuario recién registrado no tiene fila en `namespace`.
- La entidad `Espacio` normaliza el nombre a minúsculas al construirse (`Melina` → `melina`), y `EspacioRepository` busca con el nombre normalizado. Por eso `Melina`, `MELINA` y `melina` chocan con la misma restricción.
- Un nombre repetido se traduce a `EspacioOcupadoException`, con el mismo criterio que las colisiones de alias de [ADR-0024](0024-capa-repository-con-entitymanager.md): solo la restricción `uk_namespace_name`. Otros errores conservan su causa.
- Código en dos paquetes hermanos de `enlace` ([ADR-0006](0006-paquetes-por-funcionalidad.md)): `app.usuario` y `app.espacio`. El espacio va aparte porque también lo usarán los enlaces.

## Alternativas consideradas
- **El espacio como columna de `app_user`:** más simple, pero mezcla la identidad de Google con el nombre público, y los enlaces del Camino 2 necesitan referenciar el espacio.
- **Unicidad solo en el código:** no la garantiza ante pedidos simultáneos, y el Cliente pide que la garantice la base.
- **Comparar sin distinguir mayúsculas en la consulta:** se prefirió guardar ya normalizado, como pide el Cliente, para que la restricción `UNIQUE` funcione sola.

## Consecuencias
- Pedir un espacio ocupado falla en la base aunque dos pedidos lleguen a la vez.
- La tabla `link` no cambia en este paso. Cómo se relacionan los enlaces con el espacio se decide en el paso 2.4, cuando se creen enlaces dentro del espacio.
- El cambio de espacio (paso 2.3) deberá hacer que los enlaces del anterior dejen de funcionar.
- La traducción de colisiones queda duplicada en `EnlaceRepository` y `EspacioRepository` (un ciclo de pocas líneas). Si aparece un tercer caso, conviene extraerla a `app.common`.

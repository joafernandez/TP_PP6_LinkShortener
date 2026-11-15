# 008 – Transición de 60 minutos al cambiar de espacio

- **Fecha:** 2026-11-15
- **Herramienta:** Claude Code (VS Code)
- **Objetivo:** que, al cambiar de espacio, los enlaces funcionen con el nombre nuevo y el anterior siga funcionando y reservado durante 60 minutos.
- **Estado:** implementado y verificado con tests y con el servidor real, en la rama `feature/etapa2-transicion-espacio`. Sin commit.

## Prompt (resumido)

El Cliente cambió el requerimiento: el espacio anterior ya no deja de funcionar en el acto. El nombre nuevo funciona de inmediato con todos los enlaces; el anterior convive 60 minutos (desde el cambio) y queda reservado; el cambio no renueva el vencimiento de los enlaces (dos vencimientos independientes); la redirección sigue siendo un 302 sin pantalla intermedia; el dueño ve en «Tu espacio» el tiempo real que le queda a cada nombre anterior. La reserva debe ser segura en la base, también con pedidos concurrentes.

Primero se pidió solo el análisis: si era posible y qué se rompía. Después el equipo aprobó el plan y resolvió las tres dudas planteadas:
1. **Varios cambios seguidos:** se conservan todos los nombres anteriores, cada uno con su propio vencimiento.
2. **Volver a un nombre propio reservado:** se puede; vuelve a ser principal y deja de vencer. El que se abandona empieza su transición; los demás conservan su vencimiento.
3. **QR de enlaces anteriores:** siguen funcionando mientras el nombre y el enlace estén vigentes, sin regenerarse.

## Diseño antes del código

- ADR-0034 y contrato OpenAPI (`previousNamespaces` en `UserResponse` y `NamespaceResponse`, esquema `PreviousNamespace`).
- Tabla `namespace_name` (migración `V5`) con **todos** los nombres en una columna `UNIQUE`: el principal (sin vencimiento) y los anteriores (con vencimiento). Así la base impide que dos espacios tengan o reserven el mismo nombre. La migración copia los nombres actuales como principales.
- Reclamar un nombre: insertar si es libre; recuperarlo si es propio y vigente; reasignarlo con un `UPDATE` condicional si su transición venció; si no, 409.
- Los enlaces no se tocan: siguen atados al espacio y conservan su vencimiento. Se elimina el vencimiento de enlaces al cambiar (ADR-0031 y 0032).
- La redirección y el QR buscan el espacio por cualquier nombre vigente. El QR codifica la URL con el nombre pedido.
- Duración configurable: `app.namespace.transition=60m`.

## Cambios

| Tipo | Archivos |
|---|---|
| Base de datos | `V5__create_namespace_name.sql` |
| Código nuevo | `espacio/NombreDeEspacio`, `NombreDeEspacioRepository`, `NombreAnterior`, `dto/NombreAnteriorResponse` |
| Código modificado | `EspacioService` (reclamar, transición, sin vencer enlaces), `EspacioController` y `dto/EspacioResponse` (nombres anteriores), `EnlaceDeEspacioService` (resolución por nombre vigente y URL con el nombre pedido), `EnlaceDeEspacioRepository` (se eliminan `vencerVigentes` y `buscarPorNombreDeEspacio`, que quedaron sin uso), `QrController`, `UsuarioService`, `PerfilUsuario`, `dto/UsuarioResponse`, `AppProperties` (`app.namespace.transition`) y `application.properties` de main y test |
| Web | `index.html`, `assets/app.js`, `assets/styles.css`: aviso por nombre anterior con los minutos restantes, actualizado cada 15 segundos, que pasa a "ya expiró" al vencer |
| Tests | `TransicionDeEspacioIT` (10, nuevo), `EspacioServiceTest` (8, reescrito), `UsuarioServiceTest` y `ValidadorEspacioTest` (firmas nuevas). Se eliminaron 3 tests que verificaban la regla anterior (en `EnlacesDeEspacioIT` y `EspacioControllerIT`), reemplazados por `TransicionDeEspacioIT` |
| Documentación | ADR-0034; ADR-0031 y 0032 marcados como parcialmente reemplazados; `openapi.yaml`; índices; `CLAUDE.md` |

## Verificación

- Primera corrida: 2 tests nuevos fallaron por un error del test, no del código: el **enlace** ya había vencido (60 minutos desde su creación) cuando el test esperaba que siguiera funcionando, justo la regla de vencimientos independientes. Se corrigieron creando el enlace más tarde.
- `gradlew clean test`: **261 tests**, sin fallos. JaCoCo: **98,22 % de instrucciones**, **97,88 % de líneas**, **91,58 % de ramas** (umbral: 90/90/85).
- `TransicionDeEspacioIT` cubre: los dos nombres llevan a los mismos enlaces, sin duplicarlos; los enlaces nuevos usan el principal; el anterior deja de funcionar exactamente a los 60 minutos y el nuevo sigue; el cambio no renueva el vencimiento de los enlaces; el nombre anterior queda reservado (409) y se libera a los 60 minutos, y quien lo toma no hereda enlaces; varios cambios con vencimientos propios; volver a un nombre propio reservado; perfil con los nombres anteriores, que desaparecen al expirar; QR con el nombre anterior hasta el vencimiento; la base rechaza un nombre duplicado.
- Sintaxis de `app.js` revisada con `node --check`. No hay tests automáticos de la web: el aviso se prueba a mano.
- Servidor real: Flyway aplicó la `V5` sobre la base existente; web 200 con el aviso nuevo; alta anónima 201; `/v3/api-docs` publica `previousNamespaces`.

## Pendiente

- Prueba manual del aviso en «Tu espacio» con una cuenta real.
- Commit, push y merge, cuando el equipo los autorice.
